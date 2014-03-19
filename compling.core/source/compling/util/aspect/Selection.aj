package compling.util.aspect;

import compling.parser.ecgparser.LeftCornerParser;

public aspect Selection {

	pointcut parserTables(): call(new(..)) && withincode(LeftCornerParser.new(..));

	Object around(): parserTables() {
		System.out.printf("Table %s about to be created . . . ", thisJoinPoint.toShortString());

		long startTime = System.currentTimeMillis();
		Object ret = proceed();

		System.out.printf("done, %.2f s.\n", (System.currentTimeMillis() - startTime) / 1000.);

		return ret;
	}
}
