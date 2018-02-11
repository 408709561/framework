/*
 *
 *  *  Copyright (C) 2018  Wanghaobin<463540703@qq.com>
 *
 *  *  AG-Enterprise 企业版源码
 *  *  郑重声明:
 *  *  如果你从其他途径获取到，请告知老A传播人，奖励1000。
 *  *  老A将追究授予人和传播人的法律责任!
 *
 *  *  This program is free software; you can redistribute it and/or modify
 *  *  it under the terms of the GNU General Public License as published by
 *  *  the Free Software Foundation; either version 2 of the License, or
 *  *  (at your option) any later version.
 *
 *  *  This program is distributed in the hope that it will be useful,
 *  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *  GNU General Public License for more details.
 *
 *  *  You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package com.github.wxiaoqi.security.common.depart;

import com.github.ag.core.context.BaseContextHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.io.StringReader;
import java.sql.Connection;
import java.util.List;
import java.util.Properties;

/**
 * 租户数据隔离拦截器
 *
 * @author ace
 * @create 2018/2/9.
 */
@Intercepts({@Signature(method = "prepare", type = StatementHandler.class, args = {Connection.class, Integer.class})})
public class DepartMybatisInterceptor implements Interceptor {
    private IUserDepartDataService userDepartDataService;

    public DepartMybatisInterceptor(IUserDepartDataService userDepartDataService) {
        this.userDepartDataService = userDepartDataService;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        //由于mappedStatement中有我们需要的方法id,但却是protected的，所以要通过反射获取
        MetaObject statementHandler = SystemMetaObject.forObject(handler);
        MappedStatement mappedStatement = (MappedStatement) statementHandler.getValue("delegate.mappedStatement");
        if (!SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {
            return invocation.proceed();
        }
//        if (handler.getBoundSql().getSql().startsWith("SELECT count(0) FROM")) {
//            return invocation.proceed();
//        }
        String namespace = mappedStatement.getId();
        String className = namespace.substring(0, namespace.lastIndexOf("."));
        Class<?> clazz = Class.forName(className);
        Depart annotation = clazz.getAnnotation(Depart.class);
        // 租户数据隔离
        if (annotation != null) {
            //获取sql
            BoundSql boundSql = handler.getBoundSql();
            String sql = boundSql.getSql();
            CCJSqlParserManager parserManager = new CCJSqlParserManager();
            Select select = (Select) parserManager.parse(new StringReader(sql));
            PlainSelect plain = (PlainSelect) select.getSelectBody();
            Expression where = plain.getWhere();
            StringBuffer whereSql = new StringBuffer("1 = 0 ");
            // 添加用户自己的查询条件
            whereSql.append(" or ").append(annotation.userField()).append(" = '").append(BaseContextHandler.getUserID()).append("'");
            // 拼接部门数据sql
            if (userDepartDataService != null) {
                List<String> userDataDepartIds = userDepartDataService.getUserDataDepartIds(BaseContextHandler.getUserID());
                if (userDataDepartIds != null && userDataDepartIds.size() > 0) {
                    for (int i = 0; i < userDataDepartIds.size(); i++) {
                        if (i == 0) {
                            whereSql.append(" or ");
                        }
                        whereSql.append(annotation.departField()).append(" = '").append(userDataDepartIds.get(i)).append("' ");
                    }
                }
            }
            Expression expression = CCJSqlParserUtil
                    .parseCondExpression(whereSql.toString());
            if (where == null) {
                Expression whereExpression = (Expression) expression;
                plain.setWhere(whereExpression);
            } else {
                expression = CCJSqlParserUtil
                        .parseCondExpression(expression.toString() + " and ( " + where.toString() + " )");
                plain.setWhere(expression);
            }
            statementHandler.setValue("delegate.boundSql.sql", select.toString());
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
