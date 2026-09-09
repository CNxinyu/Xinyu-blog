package com.xinyu.javabackend;

import com.xinyu.common.api.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaBackendApplicationTests {

	@Test
	void foundationResponseIsAvailableWithoutExternalServices() {
		assertThat(ApiResponse.success("ok").code()).isZero();
	}

}
