package tech.wetech.admin.shiro;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import tech.wetech.admin.model.Result;
import tech.wetech.admin.model.enumeration.CommonResultStatus;
import tech.wetech.admin.utils.JsonUtil;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 自定义登录过滤器返回json，默认是返回视图
 *
 * @author cjbi
 */
@Slf4j
public class JsonAuthenticationFilter extends AuthenticatingFilter {

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        return null;
    }

    /**
     * 登录成功
     *
     * @param token
     * @param subject
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) {
        if (log.isDebugEnabled()) {
            log.debug("用户{}登录成功", token.getPrincipal());
        }
        HttpServletResponse res = (HttpServletResponse) response;
        res.setContentType("application/json;charset=utf-8");
        try (PrintWriter out = response.getWriter()) {
            JsonUtil json = JsonUtil.getInstance();
            out.println(json.obj2json(Result.success()));
            out.flush();
        } catch (IOException e) {
        }
        return false;
    }

    /**
     * 登录失败
     *
     * @param token
     * @param e
     * @param request
     * @param response
     * @return
     */
    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        if (log.isDebugEnabled()) {
            log.debug("用户{}登录失败", token.getPrincipal(), e);
        }
        HttpServletResponse res = (HttpServletResponse) response;
        res.setContentType("application/json;charset=utf-8");
        String message = null;
        if (e instanceof UnknownAccountException) {
            message = "账号不存在";
        } else if (e instanceof IncorrectCredentialsException) {
            message = "密码错误";
        } else if (e instanceof ExcessiveAttemptsException) {
            message = "登陆失败次数过多";
        } else {
            message = "其他错误：" + e.getMessage();
        }
        try (PrintWriter out = response.getWriter()) {
            Result failureResult = Result.failure(CommonResultStatus.LOGIN_ERROR, message);
            JsonUtil json = JsonUtil.getInstance();
            out.println(json.obj2json(failureResult));
            out.flush();
        } catch (IOException ex) {
        }
        return false;
    }

    protected boolean isLoginSubmission(ServletRequest request, ServletResponse response) {
        return (request instanceof HttpServletRequest) && WebUtils.toHttp(request).getMethod().equalsIgnoreCase(POST_METHOD);
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        //return false 拦截， true 放行
        HttpServletResponse res = (HttpServletResponse) response;
        if (this.isLoginRequest(request, response)) {
            if (this.isLoginSubmission(request, response)) {
                if (log.isTraceEnabled()) {
                    log.trace("Login submission detected.  Attempting to execute login.");
                }
                return this.executeLogin(request, response);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Login page view.");
                }
                //allow them to see the login page ;)
                return true;
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Attempting to access a path which requires authentication.  Forwarding to the " +
                        "Authentication url [" + getLoginUrl() + "]");
            }
            res.setContentType("application/json;charset=utf-8");
            try (PrintWriter out = response.getWriter()) {
                Result failureResult = Result.failure(CommonResultStatus.LOGIN_ERROR, "请先登录再操作");
                out.println(JsonUtil.getInstance().obj2json(failureResult));
                out.flush();
            }
            return false;
        }
    }

}
