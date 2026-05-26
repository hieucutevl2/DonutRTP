package hieucutevl.donutrtp.utils.random;

import java.util.concurrent.ThreadLocalRandom;

public class GetRandomPoint {
   public static RandomPoint getRandomPointOnSquare(int startRadius, int endRadius, int originX, int originY) {
      ThreadLocalRandom random = ThreadLocalRandom.current();

      int x;
      int y;
      do {
         x = random.nextInt(-endRadius, endRadius + 1);
         y = random.nextInt(-endRadius, endRadius + 1);
      } while(Math.abs(x) < startRadius && Math.abs(y) < startRadius);

      return new RandomPoint(originX + x, originY + y);
   }
}
