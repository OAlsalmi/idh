package lcc;

import edu.mines.jtk.dsp.SincInterpolator;

import static edu.mines.jtk.util.MathPlus.*;

/**
 * Synthetic displacements for 3-D images.
 * @author Dave Hale, Colorado School of Mines
 * @version 2006.11.26
 */
public abstract class Displacement3 {

  public static Displacement3 constant(
    double u1, double u2, double u3, int n1, int n2, int n3) 
  {
    return new ConstantDisplacement3(u1,u2,u3,n1,n2,n3);
  }

  public static Displacement3 gaussian(
    double u1, double u2, double u3, int n1, int n2, int n3) 
  {
    return new GaussianDisplacement3(u1,u2,u3,n1,n2,n3);
  }

  public static Displacement3 sinusoid(
    double u1, double u2, double u3, int n1, int n2, int n3) 
  {
    return new SinusoidDisplacement3(u1,u2,u3,n1,n2,n3);
  }

  public abstract double u1(double x1, double x2, double x3);

  public abstract double u2(double x1, double x2, double x3);

  public abstract double u3(double x1, double x2, double x3);

  public double u1x(double x1, double x2, double x3) {
    return u1(x1,x2,x3);
  }

  public double u2x(double x1, double x2, double x3) {
    return u2(x1,x2,x3);
  }

  public double u3x(double x1, double x2, double x3) {
    return u3(x1,x2,x3);
  }

  public double u1m(double m1, double m2, double m3) {
    double u1p;
    double u1m = 0.0;
    double u2m = 0.0;
    double u3m = 0.0;
    do {
      u1p = u1m;
      u1m = u1(m1-0.5*u1m,m2-0.5*u2m,m3-0.5*u3m);
      u2m = u2(m1-0.5*u1m,m2-0.5*u2m,m3-0.5*u3m);
      u3m = u3(m1-0.5*u1m,m2-0.5*u2m,m3-0.5*u3m);
    } while (abs(u1m-u1p)>0.0001);
    return u1m;
  }

  public double u2m(double m1, double m2, double m3) {
    double u2p;
    double u1m = 0.0;
    double u2m = 0.0;
    double u3m = 0.0;
    do {
      u2p = u2m;
      u1m = u1(m1-0.5*u1m,m2-0.5*u2m,m3-0.5*u3m);
      u2m = u2(m1-0.5*u1m,m2-0.5*u2m,m3-0.5*u3m);
      u3m = u3(m1-0.5*u1m,m2-0.5*u2m,m3-0.5*u3m);
    } while (abs(u2m-u2p)>0.0001);
    return u2m;
  }

  public double u3m(double m1, double m2, double m3) {
    double u3p;
    double u1m = 0.0;
    double u2m = 0.0;
    double u3m = 0.0;
    do {
      u3p = u3m;
      u1m = u1(m1-0.5*u1m,m2-0.5*u2m,m3-0.5*u3m);
      u2m = u2(m1-0.5*u1m,m2-0.5*u2m,m3-0.5*u3m);
      u3m = u3(m1-0.5*u1m,m2-0.5*u2m,m3-0.5*u3m);
    } while (abs(u3m-u3p)>0.0001);
    return u3m;
  }

  public double u1y(double y1, double y2, double y3) {
    double u1p;
    double u1y = 0.0;
    double u2y = 0.0;
    double u3y = 0.0;
    do {
      u1p = u1y;
      u1y = u1(y1-u1y,y2-u2y,y3-u3y);
      u2y = u2(y1-u1y,y2-u2y,y3-u3y);
      u3y = u3(y1-u1y,y2-u2y,y3-u3y);
    } while (abs(u1y-u1p)>0.0001);
    return u1y;
  }

  public double u2y(double y1, double y2, double y3) {
    double u2p;
    double u1y = 0.0;
    double u2y = 0.0;
    double u3y = 0.0;
    do {
      u2p = u2y;
      u1y = u1(y1-u1y,y2-u2y,y3-u3y);
      u2y = u2(y1-u1y,y2-u2y,y3-u3y);
      u3y = u3(y1-u1y,y2-u2y,y3-u3y);
    } while (abs(u2y-u2p)>0.0001);
    return u2y;
  }

  public double u3y(double y1, double y2, double y3) {
    double u3p;
    double u1y = 0.0;
    double u2y = 0.0;
    double u3y = 0.0;
    do {
      u3p = u3y;
      u1y = u1(y1-u1y,y2-u2y,y3-u3y);
      u2y = u2(y1-u1y,y2-u2y,y3-u3y);
      u3y = u3(y1-u1y,y2-u2y,y3-u3y);
    } while (abs(u3y-u3p)>0.0001);
    return u3y;
  }

  public float[][][] u1x() {
    float[][][] u = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double x3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double x2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double x1 = i1;
          u[i3][i2][i1] = (float)u1x(x1,x2,x3);
        }
      }
    }
    return u;
  }

  public float[][][] u2x() {
    float[][][] u = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double x3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double x2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double x1 = i1;
          u[i3][i2][i1] = (float)u2x(x1,x2,x3);
        }
      }
    }
    return u;
  }

  public float[][][] u3x() {
    float[][][] u = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double x3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double x2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double x1 = i1;
          u[i3][i2][i1] = (float)u3x(x1,x2,x3);
        }
      }
    }
    return u;
  }

  public float[][][] u1m() {
    float[][][] u = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double m3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double m2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double m1 = i1;
          u[i3][i2][i1] = (float)u1m(m1,m2,m3);
        }
      }
    }
    return u;
  }

  public float[][][] u2m() {
    float[][][] u = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double m3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double m2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double m1 = i1;
          u[i3][i2][i1] = (float)u2m(m1,m2,m3);
        }
      }
    }
    return u;
  }

  public float[][][] u3m() {
    float[][][] u = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double m3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double m2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double m1 = i1;
          u[i3][i2][i1] = (float)u3m(m1,m2,m3);
        }
      }
    }
    return u;
  }

  public float[][][] u1y() {
    float[][][] u = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double y3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double y2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double y1 = i1;
          u[i3][i2][i1] = (float)u1y(y1,y2,y3);
        }
      }
    }
    return u;
  }

  public float[][][] u2y() {
    float[][][] u = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double y3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double y2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double y1 = i1;
          u[i3][i2][i1] = (float)u2y(y1,y2,y3);
        }
      }
    }
    return u;
  }

  public float[][][] u3y() {
    float[][][] u = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double y3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double y2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double y1 = i1;
          u[i3][i2][i1] = (float)u3y(y1,y2,y3);
        }
      }
    }
    return u;
  }

  public float[][][] warp(float[][][] f) {
    SincInterpolator si = new SincInterpolator();
    float[][][] g = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double y3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double y2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double y1 = i1;
          double x1 = y1-u1y(y1,y2,y3);
          double x2 = y2-u2y(y1,y2,y3);
          double x3 = y3-u3y(y1,y2,y3);
          g[i3][i2][i1] = si.interpolate(
            _n1,1.0,0.0,_n2,1.0,0.0,_n3,1.0,0.0,f,x1,x2,x3);
        }
      }
    }
    return g;
  }

  public float[][][] unwarp(float[][][] g) {
    SincInterpolator si = new SincInterpolator();
    float[][][] f = new float[_n3][_n2][_n1];
    for (int i3=0; i3<_n3; ++i3) {
      double x3 = i3;
      for (int i2=0; i2<_n2; ++i2) {
        double x2 = i2;
        for (int i1=0; i1<_n1; ++i1) {
          double x1 = i1;
          double y1 = x1+u1x(x1,x2,x3);
          double y2 = x2+u2x(x1,x2,x3);
          double y3 = x3+u3x(x1,x2,x3);
          f[i3][i2][i1] = si.interpolate(
            _n1,1.0,0.0,_n2,1.0,0.0,_n3,1.0,0.0,g,y1,y2,y3);
        }
      }
    }
    return f;
  }

  protected Displacement3(int n1, int n2, int n3) {
    _n1 = n1;
    _n2 = n2;
    _n3 = n3;
  }

  private int _n1,_n2,_n3;

  /**
   * Constant (zero-strain) displacement.
   */
  private static class ConstantDisplacement3 extends Displacement3 {
    public ConstantDisplacement3(
      double u1, double u2, double u3, int n1, int n2, int n3) 
    {
      super(n1,n2,n3);
      _u1 = u1;
      _u2 = u2;
      _u3 = u3;
    }
    public double u1(double x1, double x2, double x3) {
      return _u1;
    }
    public double u2(double x1, double x2, double x3) {
      return _u2;
    }
    public double u3(double x1, double x2, double x3) {
      return _u3;
    }
    private double _u1,_u2,_u3;
  }

  /**
   * Derivative-of-Gaussian displacement.
   */
  private static class GaussianDisplacement3 extends Displacement3 {
    public GaussianDisplacement3(
      double u1, double u2, double u3, int n1, int n2, int n3) 
    {
      super(n1,n2,n3);
      _a1 = (n1-1)/2.0;
      _a2 = (n2-1)/2.0;
      _a3 = (n3-1)/2.0;
      _b1 = _a1/3.0;
      _b2 = _a2/3.0;
      _b3 = _a3/3.0;
      _c1 = u1*exp(0.5)/_b1;
      _c2 = u2*exp(0.5)/_b2;
      _c3 = u3*exp(0.5)/_b3;
    }
    public double u1(double x1, double x2, double x3) {
      double xa1 = x1-_a1;
      double xa2 = x2-_a2;
      double xa3 = x3-_a3;
      return -_c1*xa1*exp(-0.5*((xa1*xa1)/(_b1*_b1) +
                                (xa2*xa2)/(_b2*_b2) +
                                (xa3*xa3)/(_b3*_b3)));
    }
    public double u2(double x1, double x2, double x3) {
      double xa1 = x1-_a1;
      double xa2 = x2-_a2;
      double xa3 = x3-_a3;
      return -_c2*xa2*exp(-0.5*((xa1*xa1)/(_b1*_b1) +
                                (xa2*xa2)/(_b2*_b2) +
                                (xa3*xa3)/(_b3*_b3)));
    }
    public double u3(double x1, double x2, double x3) {
      double xa1 = x1-_a1;
      double xa2 = x2-_a2;
      double xa3 = x3-_a3;
      return -_c3*xa3*exp(-0.5*((xa1*xa1)/(_b1*_b1) +
                                (xa2*xa2)/(_b2*_b2) +
                                (xa3*xa3)/(_b3*_b3)));
    }
    private double _a1,_a2,_a3;
    private double _b1,_b2,_b3;
    private double _c1,_c2,_c3;
  }

  /**
   * Sinusoid displacement.
   */
  private static class SinusoidDisplacement3 extends Displacement3 {
    public SinusoidDisplacement3(
      double u1, double u2, double u3, int n1, int n2, int n3) 
    {
      super(n1,n2,n3);
      double l1 = n1-1;
      double l2 = n2-1;
      double l3 = n3-1;
      _a1 = u1;
      _a2 = u2;
      _a3 = u3;
      _b1 = 2.0*PI/l1;
      _b2 = 2.0*PI/l2;
      _b3 = 2.0*PI/l3;
    }
    public double u1(double x1, double x2, double x3) {
      return _a1*sin(_b1*x1)*sin(0.5*_b2*x2)*sin(0.5*_b3*x3);
    }
    public double u2(double x1, double x2, double x3) {
      return _a2*sin(0.5*_b1*x1)*sin(_b2*x2)*sin(0.5*_b3*x3);
    }
    public double u3(double x1, double x2, double x3) {
      return _a3*sin(0.5*_b1*x1)*sin(0.5*_b2*x2)*sin(_b3*x3);
    }
    private double _a1,_a2,_a3;
    private double _b1,_b2,_b3;
  }
}
