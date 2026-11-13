
package fr.abdias.incident_api.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

	private final RateLimitingFilter filter = new RateLimitingFilter();

	@Test
	void shouldAllowRequestsOutsideAuthPaths() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/incidents");
		request.setServletPath("/api/incidents");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isSameAs(request);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldRejectRequestsExceedingLimitOnAuthPaths() throws Exception {
		String ip = "203.0.113.42";

		for (int i = 0; i < 10; i++) {
			MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
			request.setServletPath("/api/auth/login");
			request.setRemoteAddr(ip);
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();

			filter.doFilter(request, response, chain);

			assertThat(chain.getRequest()).isSameAs(request);
			assertThat(response.getStatus()).isEqualTo(200);
		}

		MockHttpServletRequest eleventhRequest = new MockHttpServletRequest("POST", "/api/auth/login");
		eleventhRequest.setServletPath("/api/auth/login");
		eleventhRequest.setRemoteAddr(ip);
		MockHttpServletResponse eleventhResponse = new MockHttpServletResponse();
		MockFilterChain eleventhChain = new MockFilterChain();

		filter.doFilter(eleventhRequest, eleventhResponse, eleventhChain);

		assertThat(eleventhChain.getRequest()).isNull();
		assertThat(eleventhResponse.getStatus()).isEqualTo(429);
	}

	@Test
	void shouldTrackDifferentIpsIndependently() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
		request.setServletPath("/api/auth/login");
		request.setRemoteAddr("198.51.100.7");
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterChain chain = new MockFilterChain();
		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isSameAs(request);
		assertThat(response.getStatus()).isEqualTo(200);
	}
}
