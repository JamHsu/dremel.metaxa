package dremel.common.loggingaspect;

import java.util.logging.Logger;

public aspect LoggingAspect {
	private Logger _logger = Logger.getLogger("trace");
}
