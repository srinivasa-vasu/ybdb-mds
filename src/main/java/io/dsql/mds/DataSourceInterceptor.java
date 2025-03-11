package io.dsql.mds;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static io.dsql.mds.DataSourceInterceptor.DataSourceContextHolder.READ_ONLY;
import static io.dsql.mds.DataSourceInterceptor.DataSourceContextHolder.READ_WRITE;
import static io.dsql.mds.DataSourceInterceptor.DataSourceContextHolder.setDataSourceType;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceInterceptor {

	@Around("@annotation(org.springframework.transaction.annotation.Transactional)")
	public Object proceed(ProceedingJoinPoint jp) throws Throwable {
		Method method = ((MethodSignature) jp.getSignature()).getMethod();
		Transactional txn = method.getAnnotation(Transactional.class);
		setDataSourceType((txn != null && txn.readOnly()) ? READ_ONLY : READ_WRITE);
		return jp.proceed();
	}

	static class DataSourceContextHolder {
		public static final String READ_ONLY = "READ";
		public static final String READ_WRITE = "WRITE";
		private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

		public static void setDataSourceType(String dataSourceType) {
			CONTEXT.set(dataSourceType);
		}

		public static String getDataSourceType() {
			return CONTEXT.get();
		}

		public static void clear() {
			CONTEXT.remove();
		}
	}
}

