#############################################################################
# Testing with Rick's rock peel.

import sys
from java.awt import *
from java.awt.image import *
from java.io import *
from java.lang import *
from java.util import *
from java.nio import *
from javax.swing import *

from edu.mines.jtk.awt import *
from edu.mines.jtk.dsp import *
from edu.mines.jtk.io import *
from edu.mines.jtk.mosaic import *
from edu.mines.jtk.util import *
from edu.mines.jtk.util.ArrayMath import *

from fault import FaultScanner2,FaultSemblance

#############################################################################

#pngDir = "./png"
pngDir = None

sigmaTheta = 20
smoother = FaultScanner2.Smoother.SHEAR
#smoother = FaultScanner2.Smoother.FFT

def main(args):
  fakeImage()
  #goSlopes()
  #goAlign()
  #goSemblance()
  #goScan()
  #goThin()
  #goShifts()

def fakeImage():
  n1,d1,f1 = 1878,0.001,0.000
  n2,d2,f2 = 2576,0.001,0.000
  s1 = Sampling(n1,d1,f1)
  s2 = Sampling(n2,d2,f2)
  fileName = "/data/seis/fake/peel.dat"
  f = readImage(n1,n2,fileName)
  plot2(s1,s2,f,title="peel")
  f = mul(sgn(f),pow(abs(f),2))
  nf,sigma = 2,4.0
  rgf = RecursiveGaussianFilter(sigma)
  g = copy(f)
  g1 = zerofloat(n1,n2)
  g2 = zerofloat(n1,n2)
  for i in range(nf):
    rgf.apply20(g,g1)
    rgf.apply02(g,g2)
    g = mul(1000.0,neg(add(g1,g2)))
  nb = int(nf*2*sigma)
  n1 -= 2*nb
  n2 -= 2*nb
  f1 += nb*d1
  f2 += nb*d2
  f = copy(n1,n2,nb,nb,f)
  g = copy(n1,n2,nb,nb,g)
  s1 = Sampling(n1,d1,f1)
  s2 = Sampling(n2,d2,f2)
  plot2(s1,s2,g,title="seis")
  return s1,s2,g

def getImage():
  return fakeImage()

def goShifts():
  s1,s2,g = getImage()
  g = slog(g)
  plot2(s1,s2,g,title="log input")
  fse = FaultSemblance()
  g = fse.taper(10,g)
  p = fse.slopes(g)
  sn,sd = fse.semblanceNumDen(p,g)
  fsc = FaultScanner2(sigmaTheta,[sn,sd],smoother)
  f,t = fsc.scan(-15,15)
  plot2(s1,s2,g,f,gmin=0,gmax=1,title="fault likelihood")
  #ff,tt = fsc.thin([f,t])
  #plot2(s1,s2,g,ff,gmin=0,gmax=1,title="fault likelihood")
  shiftMin,shiftMax = -20,20
  faults = fsc.findFaults([f,t],shiftMax-shiftMin);
  ff = faults.getLikelihoods()
  plot2(s1,s2,g,ff,gmin=0,gmax=1,title="fault likelihood")
  g = fsc.smooth(4,p,ff,g)
  plot2(s1,s2,g,ff,gmin=0,gmax=1,title="input smoothed")
  p = fse.slopes(g)
  faults.findShifts(g,p,shiftMin,shiftMax)
  faults.clean()
  s = faults.getShifts()
  print "s min =",min(s)," max =",max(s)
  plot2(s1,s2,g,s,gmin=-8,gmax=8,title="fault throws")

def goThin():
  s1,s2,g = getImage()
  g = slog(g)
  plot2(s1,s2,g,title="log input")
  fse = FaultSemblance()
  g = fse.taper(10,g)
  for iter in range(1):
    p = fse.slopes(g)
    #p = zerofloat(len(p[0]),len(p))
    sn,sd = fse.semblanceNumDen(p,g)
    fsc = FaultScanner2(sigmaTheta,[sn,sd],smoother)
    f,t = fsc.scan(-15,15)
    plot2(s1,s2,g,f,gmin=0,gmax=1,title="fault likelihood")
    #plot2(s1,s2,g,t,title="fault dip (degrees)")
    fs = copy(f); RecursiveGaussianFilter(1.0).apply00(fs,fs)
    plot2(s1,s2,g,fs,gmin=0,gmax=1,title="fault likelihood smoothed")
    ft,tt = fsc.thin([f,t])
    plot2(s1,s2,g,ft,gmin=0,gmax=1,title="fault likelihood thinned")
    plot2(s1,s2,g,tt,title="fault dip (degrees) thinned")
    g = fsc.smooth(8,p,ft,g)
    plot2(s1,s2,g,title="input smoothed")

def goScan():
  s1,s2,g = getImage()
  g = slog(g)
  fse = FaultSemblance()
  g = fse.taper(10,g)
  p = fse.slopes(g)
  sn,sd = fse.semblanceNumDen(p,g)
  fsc = FaultScanner2(sigmaTheta,[sn,sd])
  st = Sampling(31,1.0,-15.0)
  for theta in st.values:
    f = fsc.likelihood(theta)
    plot2(s1,s2,g,f,gmin=0,gmax=1,title="theta = "+str(int(theta)))
  tmin,tmax = st.first,st.last
  f,t = fsc.scan(tmin,tmax)
  plot2(s1,s2,g,f,gmin=0,gmax=1,title="fault likelihood")
  plot2(s1,s2,g,t,gmin=tmin,gmax=tmax,title="fault dip (degrees)")

def goSemblance():
  s1,s2,g = getImage()
  g = slog(g)
  fse = FaultSemblance()
  g = fse.taper(10,g)
  p = fse.slopes(g)
  sn0,sd0 = fse.semblanceNumDen(p,g)
  print "semblances for different vertical smoothings:"
  for sigma in [0,2,4,8]:
    ref = RecursiveExponentialFilter(sigma)
    sn = copy(sn0)
    sd = copy(sd0)
    ref.apply1(sn,sn)
    ref.apply1(sd,sd)
    s = fse.semblanceFromNumDen(sn,sd)
    print "sigma =",sigma," s min =",min(s)," max =",max(s)
    title = "semblance: sigma = "+str(sigma)
    plot2(s1,s2,g,s,gmin=0,gmax=1,title=title)

def goAlign():
  s1,s2,g = getImage()
  g = slog(g)
  n1,n2 = len(g[0]),len(g)
  fse = FaultSemblance()
  p = fse.slopes(g)
  ref = RecursiveExponentialFilter(4)
  sn,sd = fse.semblanceNumDen(p,g)
  ref.apply1(sn,sn)
  ref.apply1(sd,sd)
  s = fse.semblanceFromNumDen(sn,sd)
  plot2(s1,s2,g,s,gmin=0,gmax=1,title="semblance with alignment")
  p = zerofloat(n1,n2) # semblance with zero slopes
  sn,sd = fse.semblanceNumDen(p,g)
  ref.apply1(sn,sn)
  ref.apply1(sd,sd)
  s = fse.semblanceFromNumDen(sn,sd)
  plot2(s1,s2,g,s,gmin=0,gmax=1,title="semblance without alignment")

def goSlopes():
  s1,s2,g = getImage()
  plot2(s1,s2,g,title="input")
  g = slog(g)
  plot2(s1,s2,g,title="log input")
  fse = FaultSemblance()
  p = fse.slopes(g)
  plot2(s1,s2,g,p,gmin=-0.9,gmax=0.9,title="slopes")

###
def xfindShifts(sigma,min1,max1,lag2,f):
  n1,n2 = len(f[0]),len(f)
  u = zerofloat(n1,n2)
  lsf = LocalShiftFinder(sigma)
  lsf.setSmoothShifts(True)
  #lsf.setSmoothShifts(False)
  for i2 in range(n2):
    i2m = max(i2-lag2,0)
    i2p = min(i2+lag2,n2-1)
    lsf.find1(min1,max1,f[i2m],f[i2p],u[i2])
  return u

def spow(p,f):
  return mul(sgn(f),pow(abs(f),p))

def slog(f):
  return mul(sgn(f),log(add(1.0,abs(f))))

def sexp(f):
  return mul(sgn(f),sub(exp(abs(f)),1.0))
 
#############################################################################
# data read/write

def readImage(n1,n2,fileName):
  f = zerofloat(n1,n2)
  ais = ArrayInputStream(fileName)
  ais.readFloats(f)
  ais.close()
  return f

def writeImage(f,fileName):
  aos = ArrayOutputStream(fileName)
  aos.writeFloats(f)
  aos.close()

def imageTpd():
  #ft,fx = 0.500,0.000
  #dt,dx = 0.004,0.025
  f1,f2 = 0.0,0.0
  d1,d2 = 1.0,1.0
  n1,n2 = 251,357
  fileName = "/data/seis/tp/csm/oldslices/tp73.dat"
  x = readImage(n1,n2,fileName)
  s1,s2 = Sampling(n1,d1,f1),Sampling(n2,d2,f2)
  return s1,s2,x

def imageF3d():
  n1,n2 = 462,951
  d1,d2 = 0.004,0.025
  f1,f2 = 0.004,0.000
  fileName = "/data/seis/f3d/f3d75.dat"
  x = readImage(n1,n2,fileName)
  subset = True
  if subset:
    j1,j2 = 240,0
    n1,n2 = n1-j1,440
    f1,f2 = f1+j1*d1,f2+j2*d2
    x = copy(n1,n2,j1,j2,x)
  s1,s2 = Sampling(n1,d1,f1),Sampling(n2,d2,f2)
  return s1,s2,x

def imageSyn():
  s1,s2,f = imageTpd()
  n1,n2 = s1.count,s2.count
  g = zerofloat(n1,n2)
  f1 = copy(f[n2/8])
  f2 = shiftRamp(f1)
  for i2 in range(n2):
    for i1 in range(n1):
      if i2<n2/4:
        copy(f1,g[i2])
      elif i2<n2/3:
        copy(f2,g[i2])
      elif i2<2*n2/3:
        copy(f1,g[i2])
      else:
        copy(f2,g[i2])
  #zero(g[1*n2/3])
  #zero(g[2*n2/3])
  #r = randomNoise(1.0,n1,n2)
  #g = add(g,r)
  #rgf = RecursiveGaussianFilter(2.0)
  #rgf.applyX0(g,g)
  return s1,s2,g

def shiftRamp(f):
  n = len(f)
  g = copy(f)
  t = rampfloat(0.0,1.0-8.0/(n-1),n)
  si = SincInterpolator()
  si.interpolate(n,1.0,0.0,f,n,t,g)
  return g

def randomNoise(a,n1,n2):
  ran = Random(3)
  r = mul(2.0*a,sub(randfloat(ran,n1,n2),0.5))
  rgf = RecursiveGaussianFilter(1.0)
  rgf.apply00(r,r)
  return r
 
#############################################################################
# plotting

def plot2(s1,s2,f,g=None,gmin=None,gmax=None,
          label=None,title=None,png=None):
  n1 = len(f[0])
  n2 = len(f)
  panel = panel2()
  if label:
    panel.addColorBar(label)
  else:
    panel.addColorBar()
  if title:
    panel.setTitle(title)
  panel.setColorBarWidthMinimum(180)
  pv = panel.addPixels(s1,s2,f)
  #pv = panel.addPixels(f)
  pv.setInterpolation(PixelsView.Interpolation.NEAREST)
  pv.setColorModel(ColorMap.GRAY)
  #pv.setClips(-4.0,4.0)
  pv.setPercentiles(1,99)
  if g:
    alpha = 0.5
    #pv = panel.addPixels(s1,s2,g)
    pv = panel.addPixels(g)
    pv.setInterpolation(PixelsView.Interpolation.NEAREST)
    if gmin==None: gmin = min(g)
    if gmax==None: gmax = max(g)
    pv.setClips(gmin,gmax)
    pv.setColorModel(ColorMap.getJet(alpha))
    if gmin==0:
      updateColorModel(pv,0.9)
  frame2(panel,png)

def updateColorModel(pv,alpha):
    n = 256
    r = zerobyte(n)
    g = zerobyte(n)
    b = zerobyte(n)
    a = zerobyte(n)
    icm = pv.getColorModel()
    icm.getReds(r)
    icm.getGreens(g)
    icm.getBlues(b)
    for i in range(n):
      ai = int(255.0*alpha*i/n)
      if ai>127:
        ai -= 256
      a[i] = ai
    #if alpha<1.0:
      #r[n/2] = r[n/2-1] = -1
      #g[n/2] = g[n/2-1] = -1
      #b[n/2] = b[n/2-1] = -1
      #a[n/2  ] = a[n/2-1] = 0
      #a[n/2+1] = a[n/2-2] = 0
      #a[n/2+2] = a[n/2-3] = 0
      #a[0] = a[1] = 0
    icm = IndexColorModel(8,n,r,g,b,a)
    pv.setColorModel(icm)

def panel2():
  #panel = PlotPanel(1,1,
  #  PlotPanel.Orientation.X1DOWN_X2RIGHT,
  #  PlotPanel.AxesPlacement.NONE)
  panel = PlotPanel(1,1,
    PlotPanel.Orientation.X1DOWN_X2RIGHT)
  return panel

def frame2(panel,png=None):
  frame = PlotFrame(panel)
  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  #frame.setFontSizeForPrint(8,240)
  #frame.setSize(1240,774)
  #frame.setFontSizeForSlide(1.0,0.8)
  #frame.setSize(1290,777)
  #frame.setSize(1490,977)
  frame.setSize(1290,815)
  frame.setVisible(True)
  if png and pngDir:
    frame.paintToPng(400,3.2,pngDir+"/"+png+".png")
  return frame

#############################################################################
# Do everything on Swing thread.

class RunMain(Runnable):
  def run(self):
    main(sys.argv)
SwingUtilities.invokeLater(RunMain())

"""
Outline:
find slopes p
use p to align minus-plus images fm and fp
for all thetas
  shear fm and fp
  compute correlation coefficients c
  unshear c
  compute fault likelihood d
  remember dmax and corresponding theta

smooth dmax laterally and pick peaks
for all fault curves
  gather samples on both sides of fault
  cross-correlate to find displacements
"""
