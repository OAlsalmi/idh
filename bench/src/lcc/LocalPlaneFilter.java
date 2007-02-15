/****************************************************************************
Copyright (c) 2006, Colorado School of Mines and others. All rights reserved.
This program and accompanying materials are made available under the terms of
the Common Public License - v1.0, which accompanies this distribution, and is 
available at http://www.eclipse.org/legal/cpl-v10.html
****************************************************************************/
package lcc;

import edu.mines.jtk.dsp.*;
import edu.mines.jtk.la.*;
import edu.mines.jtk.util.*;
import static edu.mines.jtk.util.MathPlus.*;

/**
 * Local plane filtering of images.
 * @author Dave Hale, Colorado School of Mines
 * @version 2007.02.10
 */
public class LocalPlaneFilter {

  /**
   * Filter type.
   */
  public enum Type {
    CLAERBOUT1, // Jon Claerbout's "wavekill" filter
    FOMEL1, // Fomel's 2002 filter with coefficients a function of slope
    FOMEL2, // Fomel's filter modified with coefficients a function of angle
    HALE1, // simplest finite-difference operators
    HALE2, // average of finite-difference operators
    HALE3, // like HALE2 but with more care for variable coefficients
    HALE4, // Claerbout's wavekill filter cascaded with transpose (SPD)
    HALE5, // minimum-phase wavekill filter cascaded with transpose (SPD)
  };

  public LocalPlaneFilter(double sigma) {
    this(sigma,Type.HALE1);
  }

  public LocalPlaneFilter(double sigma, Type type) {
    this(sigma,0.0,type);
  }

  public LocalPlaneFilter(double sigma, double small, Type type) {
    _rgfGradient = new RecursiveGaussianFilter(1.0);
    _rgfSmoother = new RecursiveGaussianFilter(sigma);
    _stability = (float)(1.0+small);
    _type = type;
    if (type==Type.CLAERBOUT1) {
      _filter6 = new Claerbout1Filter();
    } else if (type==Type.FOMEL1) {
      _filter6 = new Fomel1Filter();
    } else if (type==Type.FOMEL2) {
      _filter6 = new Fomel2Filter();
    } else if (type==Type.HALE1) {
      _filter6 = new Hale1Filter();
    } else if (type==Type.HALE2) {
      _filter6 = new Hale2Filter();
    } else if (type==Type.HALE3) {
      _filter6 = new Hale3Filter();
    }
  }

  /**
   * Finds best-fitting local plane with estimate of planarity.
   * @param x array with image in which to find planar features.
   * @return array u of plane estimates. u[0] is planarity and
   *  u[1] and u[2] are the components of the unit normal vector
   *  in the 1st and 2nd dimensions.
   */
  public float[][][] find(float[][] x) {
    int n1 = x[0].length;
    int n2 = x.length;

    // Gradient.
    float[][] g1 = new float[n2][n1];
    float[][] g2 = new float[n2][n1];
    _rgfGradient.apply1X(x,g1);
    _rgfGradient.applyX1(x,g2);

    // Gradient products.
    float[][] g11 = new float[n2][n1];
    float[][] g12 = g1;
    float[][] g22 = g2;
    for (int i2=0; i2<n2; ++i2) {
      for (int i1=0; i1<n1; ++i1) {
        float g1i = g1[i2][i1];
        float g2i = g2[i2][i1];
        g11[i2][i1] = g1i*g1i;
        g12[i2][i1] = g1i*g2i;
        g22[i2][i1] = g2i*g2i;
      }
    }
    
    // Smoothed gradient products comprise the structure tensor.
    float[][] gtt = new float[n2][n1];
    _rgfSmoother.apply0X(g11,gtt);
    _rgfSmoother.applyX0(gtt,g11);
    _rgfSmoother.apply0X(g12,gtt);
    _rgfSmoother.applyX0(gtt,g12);
    _rgfSmoother.apply0X(g22,gtt);
    _rgfSmoother.applyX0(gtt,g22);

    // For each sample, the eigenvector corresponding to the largest
    // eigenvalue is normal to the plane. The size of that eigenvalue
    // is a measure of planarity in the interval [0,1].
    float[][][] u = new float[3][][];
    u[0] = g11;
    u[1] = g12;
    u[2] = g22;
    float[][] a = new float[2][2];
    float[][] v = new float[2][2];
    float[] d = new float[2];
    for (int i2=0; i2<n2; ++i2) {
      for (int i1=0; i1<n1; ++i1) {
        a[0][0] = g11[i2][i1];
        a[0][1] = g12[i2][i1];
        a[1][0] = g12[i2][i1];
        a[1][1] = g22[i2][i1];
        Eigen.solveSymmetric22(a,v,d);
        u[0][i2][i1] = (d[0]-d[1])/(d[0]+d[1]);
        float v1 = v[0][0];
        float v2 = v[0][1];
        if (v1<0.0f) {
          v1 = -v1;
          v2 = -v2;
        }
        u[1][i2][i1] = v1;
        u[2][i2][i1] = v2;
      }
    }
    return u;
  }

  /**
   * Applies this local plane filter.
   * Input and output arrays must be distinct.
   * @param u array of plane estimates.
   * @param x array with input image.
   * @param y array with output image.
   */
  public void applyForward(float[][][] u, float[][] x, float[][] y) {
    if (_filter6!=null) {
      applyForward(_filter6,u,x,y);
    } else if (_type==Type.HALE4) {
      applyForwardSpd(u,x,y);
    } else if (_type==Type.HALE5) {
      SpdMpFilter.applyForward(_stability,u,x,y);
    }
  }

  /**
   * Applies the inverse of this local plane filter.
   * @param u array of plane estimates.
   * @param x array with input image
   * @param y array with output image
   */
  public void applyInverse(float[][][] u, float[][] x, float[][] y) {
    if (_filter6!=null) {
      applyInverse(_filter6,u,x,y);
    } else if (_type==Type.HALE4) {
      applyInverseSpdPc(u,x,y);
    } else if (_type==Type.HALE5) {
      SpdMpFilter.applyInverse(_stability,u,x,y);
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // private

  private RecursiveGaussianFilter _rgfGradient; // used to find planes
  private RecursiveGaussianFilter _rgfSmoother; // used to find planes
  private float _stability; // 1 plus a small number
  private Type _type; // filter type
  private Filter6 _filter6; // for 6-coefficient filters

  /**
   * Filters defined by six coefficients for each image sample.
   * Different plane filters have different coefficients.
   * For forward filtering, the coefficients are used as follows:
   * <pre><code>
   * y[i2][i1] = c[0]*x[i2  ][i1+1]+c[1]*x[i2  ][i1]+c[2]*x[i2  ][i1-1]
   *           + c[3]*x[i2-1][i1+1]+c[4]*x[i2-1][i1]+c[5]*x[i2-1][i1-1]
   * </code></pre>
   * For inverse filtering, a corresponding tridiagonal system of 
   * equations is solved. Note that some filters may not be invertible. 
   * The corresponding finite-difference stencil is
   * <pre><code>
   *        i2-1  i2
   *   i1-1 c[5] c[2]
   *   i1   c[4] c[1]
   *   i1+1 c[3] c[0]
   * </code></pre>
   */
  private interface Filter6 {
    /**
     * Gets coefficients for specified normal unit-vector.
     * @param i1 sample index in 1st dimension.
     * @param i2 sample index in 2nd dimension.
     * @param u planarity and normal vector.
     * @param c array of computed coefficients.
     */
    public void getCoefficients(int i1, int i2, float[][][] u, float[] c);
  }

  // Simple Claerbout-Nichols wavekill filter (only 4 non-zero coefficients)
  // This quarter-plane filter is not zero-phase and not invertible.
  private static class Claerbout1Filter implements Filter6 {
    public void getCoefficients(int i1, int i2, float[][][] u, float[] c) {
      float u1 = u[1][i2][i1];
      float u2 = u[2][i2][i1];
      float um = 0.5f*(u1-u2);
      float up = 0.5f*(u1+u2);
      c[0] = 0.0f;
      c[1] = um;
      c[2] = up;
      c[3] = 0.0f;
      c[4] = -up;
      c[5] = -um;
    }
  }

  // Fomel's 2002 filter. Coefficients are infinite for vertical planes.
  // Coefficients are multipled by 2 for consistency with other filters. 
  // Not invertible for steeper planes.
  private static class Fomel1Filter implements Filter6 {
    public void getCoefficients(int i1, int i2, float[][][] u, float[] c) {
      float u1 = u[1][i2][i1];
      float u2 = u[2][i2][i1];
      if (u1<U1MIN)
        u1 = U1MIN;
      float s = u2/u1;
      c[0] = 2.0f*(1.0f-s)*(2.0f-s)/12.0f;
      c[1] = 2.0f*(2.0f+s)*(2.0f-s)/6.0f;
      c[2] = 2.0f*(1.0f+s)*(2.0f+s)/12.0f;
      c[3] = -c[2];
      c[4] = -c[1];
      c[5] = -c[0];
    }
    private static final float U1MIN = 0.001f;
  }

  // Fomel's 2002 filter with coefficients multiplied by u1.
  // This modification makes filter coefficients finite for all planes.
  // Coefficients are multipled by 2 for consistency with other filters. 
  // Not invertible for steeper planes.
  private static class Fomel2Filter implements Filter6 {
    public void getCoefficients(int i1, int i2, float[][][] u, float[] c) {
      float u1 = u[1][i2][i1];
      float u2 = u[2][i2][i1];
      float t1 = 2.0f*u1;
      c[0] = 2.0f*(u1-u2)*(t1-u2)/12.0f;
      c[1] = 2.0f*(t1+u2)*(t1-u2)/6.0f;
      c[2] = 2.0f*(u1+u2)*(t1+u2)/12.0f;
      c[3] = -c[2];
      c[4] = -c[1];
      c[5] = -c[0];
    }
  }

  // Simple half-plane four-coefficient filter with stable inverse.
  private static class Hale1Filter implements Filter6 {
    public void getCoefficients(int i1, int i2, float[][][] u, float[] c) {
      float u1 = u[1][i2][i1];
      float u2 = u[2][i2][i1];
      c[0] = -0.5f*u2*(u1+u2);
      c[1] =  1.0f;
      c[2] =  0.5f*u2*(u1-u2);
      c[3] =  0.0f;
      c[4] = -0.5f*u1*u1;
      c[5] =  0.0f;
    }
  }

  // Half-plane filter obtained by symmetric folding of zero-phase filter.
  // Coefficients are simply computed for each sample.
  // Inverse is barely stable for constant coefficients.
  private static class Hale2Filter implements Filter6 {
    public void getCoefficients(int i1, int i2, float[][][] u, float[] c) {
      float u1 = u[1][i2][i1];
      float u2 = u[2][i2][i1];
      float um = 0.5f*(u1-u2);
      float up = 0.5f*(u1+u2);
      c[0] = 2.0f*um*up;
      c[1] = 1.0f;
      c[2] = c[0];
      c[3] = -2.0f*up*up;
      c[4] = -4.0f*um*up;
      c[5] = -2.0f*um*um;
      /* Experimental coefficients derived for leaky finite-differences.
      float a1 = 0.90f;
      float a2 = a1*a1;
      float a3 = a1*a2;
      float a4 = a2*a2;
      c[0] = (a1+a3)*um*up;
      c[1] = (1.0f+a4)*um*um+2.0f*a2*up*up;
      c[2] = c[0];
      c[3] = -2.0f*a2*up*up;
      c[4] = -2.0f*(a1+a3)*um*up;
      c[5] = -2.0f*a2*um*um;
      */
    }
  }

  // Half-plane filter obtained by symmetric folding of the impulse
  // response of the cascade of a quarter-plane wavekill filter and 
  // its transpose. Coefficients are tricky and costly to compute.
  // The inverse is barely stable for constant-coefficients.
  private static class Hale3Filter implements Filter6 {
    public void getCoefficients(int i1, int i2, float[][][] u, float[] c) {
      float[][] u1 = u[1];
      float[][] u2 = u[2];
      int n1 = u1[0].length;
      int n2 = u1.length;
      int j1 = min(i1+1,n1-1);
      int j2 = min(i2+1,n2-1);
      float u1ii = u1[i2][i1];
      float u1ij = u1[i2][j1];
      float u1ji = u1[j2][i1];
      float u1jj = u1[j2][j1];
      float u2ii = u2[i2][i1];
      float u2ij = u2[i2][j1];
      float u2ji = u2[j2][i1];
      float u2jj = u2[j2][j1];
      float umii = 0.5f*(u1ii-u2ii);
      float umij = 0.5f*(u1ij-u2ij);
      float umji = 0.5f*(u1ji-u2ji);
      float umjj = 0.5f*(u1jj-u2jj);
      float upii = 0.5f*(u1ii+u2ii);
      float upij = 0.5f*(u1ij+u2ij);
      float upji = 0.5f*(u1ji+u2ji);
      float upjj = 0.5f*(u1jj+u2jj);
      c[0] = umii*upii+umji*upji;
      c[1] = umii*umii+upij*upij+upji*upji+umjj*umjj;
      c[2] = umij*upij+umjj*upjj;
      c[3] = -upij*upij-upji*upji;
      c[4] = -umii*upii-umji*upji-umij*upij-umjj*upjj;
      c[5] = -umii*umii-umjj*umjj;
      /* Experimental coefficients derived for leaky finite-differences.
      float a1 = 0.90f;
      float a2 = a1*a1;
      float a3 = a1*a2;
      float a4 = a2*a2;
      c[0] = a1*umii*upii+a3*umji*upji;
      c[1] = umii*umii+a2*(upij*upij+upji*upji)+a4*umjj*umjj;
      c[2] = a1*umij*upij+a3*umjj*upjj;
      c[3] = -a2*(upij*upij+upji*upji);
      c[4] = -a1*(umii*upii+umji*upji)-a3*(umij*upij+umjj*upjj);
      c[5] = -a2*(umii*umii+umjj*umjj);
      */
    }
  }

  // Applys a 6-coefficient filter.
  private void applyForward(
    Filter6 filter, float[][][] u, float[][] x, float[][] y) 
  {
    int n1 = x[0].length;
    int n2 = x.length;
    float[] c = new float[6];

    // For i2=0, x[i2-1][i1] = y[i2-1][i1] = x[i2][-1] = x[i2][n1] = 0.
    int i2 = 0;
    float[] x2 = x[i2];
    float[] y2 = y[i2];
    int i1 = 0;
    filter.getCoefficients(i1,i2,u,c);
    c[1] *= _stability;
    y2[i1] = c[0]*x2[i1+1]+c[1]*x2[i1];
    for (i1=1; i1<n1-1; ++i1) {
      filter.getCoefficients(i1,i2,u,c);
      c[1] *= _stability;
      y2[i1] = c[0]*x2[i1+1]+c[1]*x2[i1]+c[2]*x2[i1-1];
    }
    filter.getCoefficients(i1,i2,u,c);
    c[1] *= _stability;
    y2[i1] = c[1]*x2[i1]+c[2]*x2[i1-1];

    // For all i2>0, assume that x[i2][-1] = x[i2][n1] = 0.
    for (i2=1; i2<n2; ++i2) {
      float[] x2m = x[i2-1];
      x2 = x[i2];
      y2 = y[i2];
      i1 = 0;
      filter.getCoefficients(i1,i2,u,c);
      c[1] *= _stability;
      y2[i1] = c[0]*x2 [i1+1]+c[1]*x2 [i1]
             + c[3]*x2m[i1+1]+c[4]*x2m[i1];
      for (i1=1; i1<n1-1; ++i1) {
        filter.getCoefficients(i1,i2,u,c);
        c[1] *= _stability;
        y2[i1] = c[0]*x2 [i1+1]+c[1]*x2 [i1]+c[2]*x2 [i1-1]
               + c[3]*x2m[i1+1]+c[4]*x2m[i1]+c[5]*x2m[i1-1];
      }
      filter.getCoefficients(i1,i2,u,c);
      c[1] *= _stability;
      y2[i1] = c[1]*x2 [i1]+c[2]*x2 [i1-1]
             + c[4]*x2m[i1]+c[5]*x2m[i1-1];
    }
  }

  // Applys inverse of a 6-coefficient filter.
  private void applyInverse(
    Filter6 filter, float[][][] u, float[][] x, float[][] y) 
  {
    int n1 = x[0].length;
    int n2 = x.length;
    TridiagonalFMatrix tm = new TridiagonalFMatrix(n1);
    float[] ta = tm.a();
    float[] tb = tm.b();
    float[] tc = tm.c();
    float[] r = new float[n1];
    float[] c = new float[6];

    // For i2=0, x[i2-1][i1] = y[i2-1][i1] = x[i2][-1] = x[i2][n1] = 0.
    int i2 = 0;
    float[] x2 = x[i2];
    float[] y2 = y[i2];
    for (int i1=0; i1<n1; ++i1) {
      filter.getCoefficients(i1,i2,u,c);
      c[1] *= _stability;
      r[i1] = x2[i1];
      ta[i1] = c[2];
      tb[i1] = c[1];
      tc[i1] = c[0];
    }
    tm.solve(r,y2);

    // For all i2>0, assume that x[i2][-1] = x[i2][n1] = 0.
    for (i2=1; i2<n2; ++i2) {
      float[] y2m = y[i2-1];
      x2 = x[i2];
      y2 = y[i2];
      int i1 = 0;
      filter.getCoefficients(i1,i2,u,c);
      c[1] *= _stability;
      tb[i1] = c[1];
      tc[i1] = c[0];
      r[i1] = x2[i1]-c[3]*y2m[i1+1]-c[4]*y2m[i1];
      for (i1=1; i1<n1-1; ++i1) {
        filter.getCoefficients(i1,i2,u,c);
        c[1] *= _stability;
        ta[i1] = c[2];
        tb[i1] = c[1];
        tc[i1] = c[0];
        r[i1] = x2[i1]-c[3]*y2m[i1+1]-c[4]*y2m[i1]-c[5]*y2m[i1-1];
      }
      filter.getCoefficients(i1,i2,u,c);
      c[1] *= _stability;
      ta[i1] = c[2];
      tb[i1] = c[1];
      r[i1] = x2[i1]-c[4]*y2m[i1]-c[5]*y2m[i1-1];
      tm.solve(r,y2);
    }
  }

  // Symmetric-positive definite filter. Applies the quarter-plane
  // wavekill filter followed by the transpose of that filter. Care
  // is taken to get the transpose correct for variable coefficients.
  private void applyForwardSpd(float[][][] u, float[][] x, float[][] y) {
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] u1 = u[1];
    float[][] u2 = u[2];
    float u1i,u2i,um,up,umy,upy;
    Array.copy(x,y);

    // Apply plane filter.
    for (int i2=n2-1; i2>0; --i2) {
      float[] y2 = y[i2];
      float[] y2m = y[i2-1];
      for (int i1=n1-1; i1>0; --i1) {
        u1i = u1[i2][i1]; u2i = u2[i2][i1]; 
        um = 0.5f*(u1i-u2i); up = 0.5f*(u1i+u2i);
        y2[i1] = um*(y2[i1]-y2m[i1-1])+up*(y2[i1-1]-y2m[i1]);
      }
      u1i = u1[i2][0]; u2i = u2[i2][0]; 
      um = 0.5f*(u1i-u2i); up = 0.5f*(u1i+u2i);
      y2[0] = um*y2[0]-up*y2m[0];
    }
    for (int i1=n1-1; i1>0; --i1) {
      u1i = u1[0][i1]; u2i = u2[0][i1]; 
      um = 0.5f*(u1i-u2i); up = 0.5f*(u1i+u2i);
      y[0][i1] = um*y[0][i1]+up*y[0][i1-1];
    }
    u1i = u1[0][0]; u2i = u2[0][0]; 
    um = 0.5f*(u1i-u2i); up = 0.5f*(u1i+u2i);
    y[0][0] = um*y[0][0];

    // Apply transpose of plane filter (with care for variable coefficients).
    u1i = u1[0][0]; u2i = u2[0][0]; 
    um = 0.5f*(u1i-u2i); up = 0.5f*(u1i+u2i);
    umy = um*y[0][0]; upy = up*y[0][0];
    y[0][0] = umy;
    for (int i1=1; i1<n1; ++i1) {
      u1i = u1[0][i1]; u2i = u2[0][i1]; 
      um = 0.5f*(u1i-u2i); up = 0.5f*(u1i+u2i);
      umy = um*y[0][i1]; upy = up*y[0][i1];
      y[0][i1] = umy;
      y[0][i1-1] += upy;
    }
    for (int i2=1; i2<n2; ++i2) {
      float[] y2 = y[i2];
      float[] y2m = y[i2-1];
      u1i = u1[i2][0]; u2i = u2[i2][0]; 
      um = 0.5f*(u1i-u2i); up = 0.5f*(u1i+u2i);
      umy = um*y2[0]; upy = up*y2[0];
      y2[0] = umy;
      y2m[0] -= upy;
      for (int i1=1; i1<n1; ++i1) {
        u1i = u1[i2][i1]; u2i = u2[i2][i1]; 
        um = 0.5f*(u1i-u2i); up = 0.5f*(u1i+u2i);
        umy = um*y2[i1]; upy = up*y2[i1];
        y2[i1] = umy;
        y2[i1-1] += upy;
        y2m[i1] -= upy;
        y2m[i1-1] -= umy;
      }
    }

    // Stability.
    float small = _stability-1.0f;
    for (int i2=0; i2<n2; ++i2) {
      float[] x2 = x[i2];
      float[] y2 = y[i2];
      for (int i1=0; i1<n1; ++i1)
        y2[i1] += small*x2[i1];
    }
  }

  // Inverse of symmetric positive-definite filter without pre-conditioning.
  private void applyInverseSpd(float[][][] u, float[][] x, float[][] y) {
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] r = new float[n2][n1]; // r
    float[][] s = new float[n2][n1]; // d
    float[][] t = new float[n2][n1]; // q
    Array.zero(y);
    Array.copy(x,r);
    Array.copy(r,s);
    float rr = dot(r,r);
    float small = rr*0.00001f;
    //System.out.println("small="+small);
    int niter;
    for (niter=0; niter<200 && rr>small; ++niter) {
      applyForwardSpd(u,s,t);
      float alpha = rr/dot(s,t);
      saxpy( alpha,s,y);
      saxpy(-alpha,t,r);
      float rrold = rr;
      rr = dot(r,r);
      float beta = rr/rrold;
      for (int i2=0; i2<n2; ++i2) {
        float[] r2 = r[i2];
        float[] s2 = s[i2];
        for (int i1=0; i1<n1; ++i1)
          s2[i1] = r2[i1]+beta*s2[i1];
      }
      //System.out.println("niter="+niter+" rr="+rr);
    }
  }

  // Inverse of symmetric positive-definite filter with pre-conditioning.
  private void applyInverseSpdPc(float[][][] u, float[][] x, float[][] y) {
    SpdMpFilter smf = new SpdMpFilter(_stability,u);
    int n1 = x[0].length;
    int n2 = x.length;
    float[][] r = new float[n2][n1]; // r
    float[][] s = new float[n2][n1]; // d
    float[][] t = new float[n2][n1]; // q
    float[][] w = new float[n2][n1]; // s
    Array.zero(y);
    Array.copy(x,r);
    smf.applyInverse(r,s);
    float rr = dot(r,s);
    float small = rr*0.00001f;
    //System.out.println("small="+small);
    int niter;
    for (niter=0; niter<10 && rr>small; ++niter) {
      applyForwardSpd(u,s,t);
      float alpha = rr/dot(s,t);
      saxpy( alpha,s,y);
      saxpy(-alpha,t,r);
      smf.applyInverse(r,w);
      float rrold = rr;
      rr = dot(r,w);
      float beta = rr/rrold;
      for (int i2=0; i2<n2; ++i2) {
        float[] w2 = w[i2];
        float[] s2 = s[i2];
        for (int i1=0; i1<n1; ++i1)
          s2[i1] = w2[i1]+beta*s2[i1];
      }
      //System.out.println("niter="+niter+" rr="+rr);
    }
  }
  private static float dot(float[][] x, float[][] y) {
    int n1 = x[0].length;
    int n2 = x.length;
    double s = 0.0;
    for (int i2=0; i2<n2; ++i2) {
      float[] x2 = x[i2];
      float[] y2 = y[i2];
      for (int i1=0; i1<n1; ++i1)
        s += x2[i1]*y2[i1];
    }
    return (float)s;
  }
  private static void saxpy(float a, float[][] x, float[][] y) {
    int n1 = x[0].length;
    int n2 = x.length;
    for (int i2=0; i2<n2; ++i2) {
      float[] x2 = x[i2];
      float[] y2 = y[i2];
      for (int i1=0; i1<n1; ++i1)
        y2[i1] += a*x2[i1];
    }
  }

  // Symmetric positive-definite minimum-phase filter.
  private static class SpdMpFilter {
    SpdMpFilter(float stability, float[][][] u) {
      _a2 = new A2(stability,u);
    }
    void applyForward(float[][] x, float[][] y) {
      _lcf.apply(_a2,x,y);
      _lcf.applyTranspose(_a2,y,y);
    }
    void applyInverse(float[][] x, float[][] y) {
      _lcf.applyInverseTranspose(_a2,x,y);
      _lcf.applyInverse(_a2,y,y);
    }
    static void applyForward(
      float stability, float[][][] u, float[][] x, float[][] y) 
    {
      A2 a2 = new A2(stability,u);
      _lcf.apply(a2,x,y);
      _lcf.applyTranspose(a2,y,y);
    }
    static void applyInverse(
      float stability, float[][][] u, float[][] x, float[][] y) 
    {
      A2 a2 = new A2(stability,u);
      _lcf.applyInverseTranspose(a2,x,y);
      _lcf.applyInverse(a2,y,y);
    }
    private static class A2 implements LocalCausalFilter.A2 {
      A2(float stability, float[][][] u) {
        if (SAMPLE_THETA) {
          _index = indexTheta(u);
        } else {
          _index = indexU2(u);
        }
        _istab = 0;
        for (int istab=1; istab<NSTAB; ++istab) {
          if (abs(STABS[istab]-stability)<abs(STABS[_istab]-stability))
            _istab = istab;
        }
      }
      public void get(int i1, int i2, float[] a) {
        Array.copy(_aTable[_istab][_index[i2][i1]],a);
      }
      private int[][] _index;
      private int _istab;
    }
    private A2 _a2;
    private static boolean SAMPLE_THETA = true;
    private static int NSTAB = 3;
    private static float[] STABS = {1.1f,1.01f,1.001f};
    private static int NTHETA = 33;
    private static float FTHETA = -0.5f*FLT_PI;
    private static float DTHETA = FLT_PI/(float)(NTHETA-1);;
    private static float STHETA = 0.9999f/DTHETA;
    private static int NU2 = NTHETA;
    private static float FU2 = -1.0f;
    private static float DU2 = 2.0f/(float)(NU2-1);;
    private static float SU2 = 0.9999f/DU2;
    private static LocalCausalFilter _lcf;
    private static float[][][] _aTable = new float[NSTAB][NTHETA][];
    static {
      if (SAMPLE_THETA) {
        makeLcfTheta();
      } else {
        makeLcfU2();
      }
    }
    private static int[][] indexTheta(float[][][] u) {
      int n1 = u[0][0].length;
      int n2 = u[0].length;
      int[][] i = new int[n2][n1];
      float[][] u1 = u[1];
      float[][] u2 = u[2];
      for (int i2=0; i2<n2; ++i2) {
        for (int i1=0; i1<n1; ++i1) {
          float theta = asin(u2[i2][i1]);
          i[i2][i1] = (int)(0.5f+(theta-FTHETA)*STHETA);
        }
      }
      return i;
    }
    private static void makeLcfTheta() {
      int maxlag = 6;
      int nlag = maxlag+2+maxlag;
      int[] lag1 = new int[nlag];
      int[] lag2 = new int[nlag];
      for (int ilag=0; ilag<nlag; ++ilag) {
        lag1[ilag] = (ilag<=maxlag)?ilag:ilag-2*maxlag;
        lag2[ilag] = (ilag<=maxlag)?0:1;
      }
      for (int istab=0; istab<NSTAB; ++istab) {
        float stability = STABS[istab];
        for (int itheta=0; itheta<NTHETA; ++itheta) {
          float theta = FTHETA+itheta*DTHETA;
          float c = cos(theta);
          float s = sin(theta);
          float m12 = 0.5f*(c-s);
          float p12 = 0.5f*(c+s);
          float[][] r = {
            {    -m12*m12,   -2.0f*m12*p12,      -p12*p12},
            {2.0f*m12*p12,       stability,  2.0f*m12*p12},
            {    -p12*p12,   -2.0f*m12*p12,      -m12*m12}
          };
          CausalFilter cf = new CausalFilter(lag1,lag2);
          cf.factorWilsonBurg(100,0.000001f,r);
          _aTable[istab][itheta] = cf.getA();
        }
      }
      _lcf = new LocalCausalFilter(lag1,lag2);
    }
    private static int[][] indexU2(float[][][] u) {
      int n1 = u[0][0].length;
      int n2 = u[0].length;
      int[][] i = new int[n2][n1];
      float[][] u1 = u[1];
      float[][] u2 = u[2];
      for (int i2=0; i2<n2; ++i2) {
        for (int i1=0; i1<n1; ++i1) {
          float u2i = u2[i2][i1];
          i[i2][i1] = (int)(0.5f+(u2i-FU2)*SU2);
        }
      }
      return i;
    }
    private static void makeLcfU2() {
      int maxlag = 6;
      int nlag = maxlag+2+maxlag;
      int[] lag1 = new int[nlag];
      int[] lag2 = new int[nlag];
      for (int ilag=0; ilag<nlag; ++ilag) {
        lag1[ilag] = (ilag<=maxlag)?ilag:ilag-2*maxlag;
        lag2[ilag] = (ilag<=maxlag)?0:1;
      }
      for (int istab=0; istab<NSTAB; ++istab) {
        float stability = STABS[istab];
        for (int iu2=0; iu2<NU2; ++iu2) {
          float u2 = FU2+iu2*DU2;
          float u1 = sqrt(1.0f-u2*u2);
          float m12 = 0.5f*(u1-u2);
          float p12 = 0.5f*(u1+u2);
          float[][] r = {
            {    -m12*m12,   -2.0f*m12*p12,      -p12*p12},
            {2.0f*m12*p12,       stability,  2.0f*m12*p12},
            {    -p12*p12,   -2.0f*m12*p12,      -m12*m12}
          };
          CausalFilter cf = new CausalFilter(lag1,lag2);
          cf.factorWilsonBurg(100,0.000001f,r);
          _aTable[istab][iu2] = cf.getA();
        }
      }
      _lcf = new LocalCausalFilter(lag1,lag2);
    }
  }
}