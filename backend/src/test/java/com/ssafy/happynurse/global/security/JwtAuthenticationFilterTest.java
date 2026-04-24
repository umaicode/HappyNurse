package com.ssafy.happynurse.global.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {


    private JwtAuthenticationFilter filter;

    @BeforeEach

        SecurityContextHolder.clearContext();
    }

    @Test
        MockHttpServletRequest request = new MockHttpServletRequest();


        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
        MockHttpServletRequest request = new MockHttpServletRequest();


        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
        MockHttpServletRequest request = new MockHttpServletRequest();


        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
        MockHttpServletRequest request = new MockHttpServletRequest();


        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    }
}
