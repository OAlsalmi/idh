import sys
from math import *
from java.awt import *
from java.lang import *
from javax.swing import *
from lcc import *
from edu.mines.jtk.awt import *
from edu.mines.jtk.dsp import *
from edu.mines.jtk.io import *
from edu.mines.jtk.mosaic import *
from edu.mines.jtk.util import *

True = 1
False = 0

n1 = 301  
d1 = 0.004
f1 = 3.6
s1 = Sampling(n1,d1,f1)

n2 = 623  
d2 = 0.025
f2 = 0
s2 = Sampling(n2,d2,f2)

n3 = 367
d3 = 0.025
f3 = 0
s3 = Sampling(n3,d3,f3)

datadir = "/data/seis/sw/"

##############################################################################
# Read/write

def readFloats3(file):
  f = Array.zerofloat(n1,n2,n3)
  af = ArrayFile(datadir+file,"r")
  af.readFloats(f)
  af.close()
  return f

def writeFloats3(file,f):
  af = ArrayFile(datadir+file,"rw")
  af.writeFloats(f)
  af.close()
  return f

def readBytes3(file):
  b = Array.zerobyte(n1,n2,n3)
  af = ArrayFile(datadir+file,"r")
  af.readBytes(b)
  af.close()
  return b

def writeBytes3(file,lag):
  af = ArrayFile(datadir+file,"rw")
  af.writeBytes(lag)
  af.close()

def readFloats12(file,i3):
  f = readFloats3(file)
  return slice12(f,i3)

def readFloats13(file,i2):
  f = readFloats3(file)
  return slice13(f,i2)

def readFloats23(file,i1):
  f = readFloats3(file)
  return slice23(f,i1)

def slice12(f,i3):
  f12 = Array.copy(f[i3])
  return f12

def slice13(f,i2):
  f13 = Array.zerofloat(n1,n3)
  for i3 in range(n3):
    Array.copy(n1,f[i3][i2],f13[i3])
  return f13

def slice23(f,i1):
  f23 = Array.zerofloat(n2,n3)
  for i3 in range(n3):
    for i2 in range(n2):
      f23[i3][i2] = f[i3][i2][i1];
  return f23

##############################################################################
# Plot
colorBarWidthMinimum = 100
frameWidth=1030
frameHeight=620

def frame(panel,png):
  frame = PlotFrame(panel)
  frame.setBackground(Color.WHITE)
  frame.setFontSize(30)
  frame.setSize(frameWidth,frameHeight)
  frame.setVisible(True)
  if png!=None:
    frame.paintToPng(600,3,png)
  return frame

def plotSeis12(f,clip=0,png=None):
  print "plotSeis: min =",Array.min(f),"  max =",Array.max(f)
  panel = PlotPanel(PlotPanel.Orientation.X1DOWN_X2RIGHT)
  panel.setColorBarWidthMinimum(colorBarWidthMinimum)
  panel.setHLabel("inline (km)")
  panel.setVLabel("time (s)")
  cb = panel.addColorBar()
  pv = panel.addPixels(s1,s2,f)
  if clip!=0:
    pv.setClips(-clip,clip)
  return frame(panel,png)

def plotSeis13(f,clip=0,png=None):
  print "plotSeis: min =",Array.min(f),"  max =",Array.max(f)
  panel = PlotPanel(PlotPanel.Orientation.X1DOWN_X2RIGHT)
  panel.setColorBarWidthMinimum(colorBarWidthMinimum)
  panel.setHLabel("crossline (km)")
  panel.setVLabel("time (s)")
  cb = panel.addColorBar()
  pv = panel.addPixels(s1,s3,f)
  if clip!=0:
    pv.setClips(-clip,clip)
  return frame(panel,png)

def plotSeis23(f,clip=0,png=None):
  print "plotSeis: min =",Array.min(f),"  max =",Array.max(f)
  panel = PlotPanel(PlotPanel.Orientation.X1DOWN_X2RIGHT)
  panel.setColorBarWidthMinimum(colorBarWidthMinimum)
  panel.setHLabel("inline (km)")
  panel.setVLabel("crossline (km)")
  cb = panel.addColorBar()
  pv = panel.addPixels(s3,s2,Array.transpose(f))
  if clip!=0:
    pv.setClips(-clip,clip)
  return frame(panel,png)

def plotLags12(f,clip=0,png=None):
  print "plotLags: min =",Array.min(f),"  max =",Array.max(f)
  panel = PlotPanel(PlotPanel.Orientation.X1DOWN_X2RIGHT)
  panel.setColorBarWidthMinimum(colorBarWidthMinimum)
  panel.setHLabel("inline (km)")
  panel.setVLabel("time (s)")
  cb = panel.addColorBar()
  pv = panel.addPixels(s1,s2,f)
  if clip!=0:
    pv.setClips(-clip,clip)
  pv.setColorModel(ColorMap.RED_WHITE_BLUE);
  return frame(panel,png)

def plotLags13(f,clip=0,png=None):
  print "plotLags: min =",Array.min(f),"  max =",Array.max(f)
  panel = PlotPanel(PlotPanel.Orientation.X1DOWN_X2RIGHT)
  panel.setColorBarWidthMinimum(colorBarWidthMinimum)
  panel.setHLabel("crossline (km)")
  panel.setVLabel("time (s)")
  cb = panel.addColorBar()
  pv = panel.addPixels(s1,s3,f)
  if clip!=0:
    pv.setClips(-clip,clip)
  pv.setColorModel(ColorMap.RED_WHITE_BLUE);
  return frame(panel,png)

def plotLags23(f,clip=0,png=None):
  print "plotLags: min =",Array.min(f),"  max =",Array.max(f)
  panel = PlotPanel(PlotPanel.Orientation.X1DOWN_X2RIGHT)
  panel.setColorBarWidthMinimum(colorBarWidthMinimum)
  panel.setHLabel("inline (km)")
  panel.setVLabel("crossline (km)")
  cb = panel.addColorBar()
  pv = panel.addPixels(s3,s2,Array.transpose(f))
  if clip!=0:
    pv.setClips(-clip,clip)
  pv.setColorModel(ColorMap.RED_WHITE_BLUE);
  return frame(panel,png)

def plotU12(i3):
  u1 = readFloats12("u1.dat",i3)
  u2 = readFloats12("u2.dat",i3)
  u3 = readFloats12("u3.dat",i3)
  plotLags12(u1)
  plotLags12(u2)
  plotLags12(u3)

def plotU13(i2):
  u1 = readFloats13("u1.dat",i2)
  u2 = readFloats13("u2.dat",i2)
  u3 = readFloats13("u3.dat",i2)
  plotLags13(u1)
  plotLags13(u2)
  plotLags13(u3)

def plotU23(i1):
  u1 = readFloats23("u1.dat",i1)
  u2 = readFloats23("u2.dat",i1)
  u3 = readFloats23("u3.dat",i1)
  plotLags23(u1)
  plotLags23(u2)
  plotLags23(u3)

def plotS12(i3):
  f = readFloats12("sw02a.dat",i3)
  g = readFloats12("sw04a.dat",i3)
  f = Array.mul(0.0002,f)
  g = Array.mul(0.0002,g)
  plotSeis12(f)
  plotSeis12(g)

def plotS13(i2):
  f = readFloats13("sw02a.dat",i2)
  g = readFloats13("sw04a.dat",i2)
  f = Array.mul(0.0002,f)
  g = Array.mul(0.0002,g)
  plotSeis13(f)
  plotSeis13(g)

def plotS23(i1):
  f = readFloats23("sw02a.dat",i1)
  g = readFloats23("sw04a.dat",i1)
  f = Array.mul(0.0002,f)
  g = Array.mul(0.0002,g)
  plotSeis23(f)
  plotSeis23(g)

##############################################################################
# Find/refine lags

def findMaxLags2(lcf,f,g,min1,max1,min2,max2):
  n1 = len(f[0])
  n2 = len(f)
  l1 = Array.zerobyte(n1,n2)
  l2 = Array.zerobyte(n1,n2)
  lcf.findMaxLags(f,g,min1,max1,min2,max2,l1,l2)
  return l1,l2

def refineLags2(lcf,f,g,l1,l2):
  n1 = len(f[0])
  n2 = len(f)
  u1 = Array.zerofloat(n1,n2)
  u2 = Array.zerofloat(n1,n2)
  lcf.refineLags(f,g,l1,l2,u1,u2)
  return u1,u2

def findMaxLags3(lcf,f,g,min1,max1,min2,max2,min3,max3):
  l1 = Array.zerobyte(n1,n2,n3)
  l2 = Array.zerobyte(n1,n2,n3)
  l3 = Array.zerobyte(n1,n2,n3)
  lcf.findMaxLags(f,g,min1,max1,min2,max2,min3,max3,l1,l2,l3)
  return l1,l2,l3

def refineLags3(lcf,f,g,l1,l2,l3):
  u1 = Array.zerofloat(n1,n2,n3)
  u2 = Array.zerofloat(n1,n2,n3)
  u3 = Array.zerofloat(n1,n2,n3)
  lcf.refineLags(f,g,l1,l2,l3,u1,u2,u3)
  return u1,u2,u3

def find3():
  f = readFloats3("sw02a.dat")
  g = readFloats3("sw04a.dat")
  lcf = LocalCorrelationFilter(12,6,6)
  min1,max1 = -1,3
  min2,max2 = -1,1
  min3,max3 = -1,1
  l1,l2,l3 = findMaxLags3(lcf,f,g,min1,max1,min2,max2,min3,max3)
  writeBytes3("l1.dat",l1)
  writeBytes3("l2.dat",l2)
  writeBytes3("l3.dat",l3)

def refine3():
  f = readFloats3("sw02a.dat")
  g = readFloats3("sw04a.dat")
  l1 = readBytes3("l1.dat")
  l2 = readBytes3("l2.dat")
  l3 = readBytes3("l3.dat")
  lcf = LocalCorrelationFilter(12,6,6)
  u1,u2,u3 = refineLags3(lcf,f,g,l1,l2,l3)
  writeFloats3("u1.dat",u1)
  writeFloats3("u2.dat",u2)
  writeFloats3("u3.dat",u3)

#############################################################################
# Tests

def test2():
  f = readFloats13("sw02a.dat",288)
  g = readFloats13("sw04a.dat",288)
  f = Array.mul(0.002,f)
  g = Array.mul(0.002,g)
  f = Array.sub(f,lpf55g(8,f))
  g = Array.sub(g,lpf55g(8,g))
  plotSeis13(f)
  plotSeis13(g)
  lcf = LocalCorrelationFilter(8,8)
  l1,l2 = findMaxLags2(lcf,f,g,-2,2,-2,2)
  u1,u2 = refineLags2(lcf,f,g,l1,l2)
  plotLags13(u1)
  plotLags13(u2)

def test3():
  f = readFloats3("sw02a.dat")
  g = readFloats3("sw04a.dat")
  lcf = LocalCorrelationFilter(12,6,6)
  l1,l2,l3 = findMaxLags3(lcf,f,g,-2,2,-2,2,-2,2)
  writeBytes3("l1.dat",l1)
  writeBytes3("l2.dat",l2)
  writeBytes3("l3.dat",l3)

#############################################################################
# Research

def findAndRefine1():
  f = readFloats3("sw02a.dat")
  g = readFloats3("sw04a.dat")
  lcf = LocalCorrelationFilter(12,6,6)
  min1,max1 = -1,3
  min2,max2 =  0,0
  min3,max3 =  0,0
  l1,l2,l3 = findMaxLags3(lcf,f,g,min1,max1,min2,max2,min3,max3)
  u1 = Array.zerofloat(n1,n2,n3)
  lcf.refineLags1(f,g,l1,u1)
  writeFloats3("u1.dat",u1)

def findAndRefine13():
  f = readFloats3("sw02a.dat")
  g = readFloats3("sw04a1.dat")
  lcf = LocalCorrelationFilter(12,6,6)
  min1,max1 =  0,0
  min2,max2 =  0,0
  min3,max3 =  -1,1
  l1,l2,l3 = findMaxLags3(lcf,f,g,min1,max1,min2,max2,min3,max3)
  u3 = Array.zerofloat(n1,n2,n3)
  lcf.refineLags3(f,g,l3,u3)
  writeFloats3("u3.dat",u3)

def subtract():
  f = readFloats3("sw02a.dat")
  g = readFloats3("sw04a.dat")
  r = Array.sub(g,f)
  writeFloats3("rxx.dat",r);
  g = readFloats3("sw04a1.dat")
  r = Array.sub(g,f)
  writeFloats3("r1x.dat",r);
  g = readFloats3("sw04a13.dat")
  r = Array.sub(g,f)
  writeFloats3("r13.dat",r);

def correct1():
  si = SincInterpolator()
  f = readFloats3("sw04a.dat")
  u = readFloats3("u1.dat")
  g = Array.zerofloat(n1,n2,n3)
  s = Array.rampfloat(0.0,1.0,n1)
  t = Array.zerofloat(n1)
  si.setUniformSampling(n1,1.0,0.0)
  for i3 in range(n3):
    for i2 in range(n2):
      Array.add(s,u[i3][i2],t)
      si.setUniformSamples(f[i3][i2])
      si.interpolate(n1,t,g[i3][i2])
  writeFloats3("sw04a1.dat",g)
  f = readFloats3("sw02a.dat")
  r = Array.sub(g,f)
  writeFloats3("r1x.dat",r);

def correct3():
  si = SincInterpolator()
  f = readFloats3("sw04a.dat")
  u = readFloats3("u3.dat")
  g = Array.zerofloat(n1,n2,n3)
  s = Array.rampfloat(0.0,1.0,n3)
  t = Array.zerofloat(n3)
  f3 = Array.zerofloat(n3)
  g3 = Array.zerofloat(n3)
  u3 = Array.zerofloat(n3)
  si.setUniformSampling(n3,1.0,0.0)
  for i2 in range(n2):
    print "correct3: i2 =",i2
    for i1 in range(n1):
      for i3 in range(n3):
        f3[i3] = f[i3][i2][i1]
        u3[i3] = u[i3][i2][i1]
      Array.add(s,u3,t)
      si.setUniformSamples(f3)
      si.interpolate(n3,t,g3)
      for i3 in range(n3):
        g[i3][i2][i1] = g3[i3]
  writeFloats3("sw04a13.dat",g)

def plotU1U3(i2):
  f = readFloats3("sw02a.dat")
  g = readFloats3("sw04a.dat")
  g1 = readFloats3("sw04a1.dat")
  u1 = readFloats3("u1.dat")
  u3 = readFloats3("u3.dat")
  rxx = readFloats3("rxx.dat")
  r1x = readFloats3("r1x.dat")
  r13 = readFloats3("r13.dat")
  fs = slice13(f,i2)
  gs = slice13(g,i2)
  g1s = slice13(g1,i2)
  u1s = slice13(u1,i2)
  u3s = slice13(u3,i2)
  rxxs = slice13(rxx,i2)
  r1xs = slice13(r1x,i2)
  r13s = slice13(r13,i2)
  fs = Array.mul(0.001,fs)
  gs = Array.mul(0.001,gs)
  g1s = Array.mul(0.001,g1s)
  u1s = Array.mul(4.0,u1s)
  u3s = Array.mul(25.0,u3s)
  rxxs = Array.mul(0.001,rxxs)
  r1xs = Array.mul(0.001,r1xs)
  r13s = Array.mul(0.001,r13s)
  suffix = str(i2)+".png"
  plotSeis13(fs,5,"sf"+suffix)
  plotSeis13(gs,5,"sg"+suffix)
  plotSeis13(g1s,5,"sg1"+suffix)
  plotLags13(u1s,6,"su1"+suffix)
  plotLags13(u3s,9,"su3"+suffix)
  plotSeis13(rxxs,9,"srxx"+suffix)
  plotSeis13(r1xs,9,"sr1x"+suffix)
  plotSeis13(r13s,9,"sr13"+suffix)

def plotU123(i2):
  f = readFloats3("sw02a.dat")
  g = readFloats3("sw04a.dat")
  u1 = readFloats3("aold/u1.dat")
  u2 = readFloats3("aold/u2.dat")
  u3 = readFloats3("aold/u3.dat")
  fs = slice13(f,i2)
  gs = slice13(g,i2)
  u1s = slice13(u1,i2)
  u2s = slice13(u2,i2)
  u3s = slice13(u3,i2)
  fs = Array.mul(0.001,fs)
  gs = Array.mul(0.001,gs)
  u1s = Array.mul(4.0,u1s)
  u2s = Array.mul(25.0,u2s)
  u3s = Array.mul(25.0,u3s)
  suffix = str(i2)+".png"
  plotSeis13(fs,5,"f"+suffix)
  plotSeis13(gs,5,"g"+suffix)
  plotLags13(u1s,6,"u1"+suffix)
  plotLags13(u2s,9,"u2"+suffix)
  plotLags13(u3s,9,"u3"+suffix)

def whiten():
  f = readFloats3("sw02a.dat")
  g = readFloats3("sw04a.dat")
  sf = ShiftFinder(12,6,6)
  sf.whiten(f,f)
  sf.whiten(g,g)
  writeFloats3("w02.dat",f)
  writeFloats3("w04.dat",g)

def findShifts():
  lmin,lmax = -2,2
  f = readFloats3("w02.dat")
  g = readFloats3("w04.dat")
  n1 = len(f[0][0])
  n2 = len(f[0])
  n3 = len(f)
  sf = ShiftFinder(12,12,12)
  u1 = Array.zerofloat(n1,n2,n3)
  u2 = Array.zerofloat(n1,n2,n3)
  u3 = Array.zerofloat(n1,n2,n3)
  du = Array.zerofloat(n1,n2,n3)
  ga = Array.copy(g)
  gb = Array.copy(g)
  for i in range(2):
    print "shift1"
    sf.find1(lmin,lmax,f,ga,du)
    sf.shift1(du,ga,gb,u1,u2,u3)
    gt = ga; ga = gb; gb = gt
    writeFloats3("u1s"+str(i)+".dat",u1)
    print "shift3"
    sf.find3(lmin,lmax,f,ga,du)
    sf.shift3(du,ga,gb,u1,u2,u3)
    gt = ga; ga = gb; gb = gt
    writeFloats3("u3s"+str(i)+".dat",u3)
    print "shift2"
    sf.find2(lmin,lmax,f,ga,du)
    sf.shift2(du,ga,gb,u1,u2,u3)
    gt = ga; ga = gb; gb = gt
    writeFloats3("u2s"+str(i)+".dat",u2)
#findShifts()

def plotSlices():
  k1 = 201 # = (4.404-3.600)/0.004
  k2 = 293 # = (7.325-0.000)/0.025

  """
  sf = readFloats3("sw02a.dat")
  sf1 = slice23(sf,k1)
  sf2 = slice13(sf,k2)
  sf = None
  plotSeis23(Array.mul(0.003,sf1),11,"sf1.png")
  plotSeis13(Array.mul(0.003,sf2),11,"sf2.png")
  sg = readFloats3("sw04a.dat")
  sg1 = slice23(sg,k1)
  sg2 = slice13(sg,k2)
  sg = None
  plotSeis23(Array.mul(0.003,sg1),11,"sg1.png")
  plotSeis13(Array.mul(0.003,sg2),11,"sg2.png")

  wf = readFloats3("w02.dat")
  wf1 = slice23(wf,k1)
  wf2 = slice13(wf,k2)
  wf = None
  plotSeis23(Array.mul(0.003,wf1),1.1,"wf1.png")
  plotSeis13(Array.mul(0.003,wf2),1.1,"wf2.png")
  wg = readFloats3("w04.dat")
  wg1 = slice23(wg,k1)
  wg2 = slice13(wg,k2)
  wg = None
  plotSeis23(Array.mul(0.003,wg1),1.1,"wg1.png")
  plotSeis13(Array.mul(0.003,wg2),1.1,"wg2.png")
  """

  u1s0 = readFloats3("u1s0.dat")
  u1s01 = slice23(u1s0,k1)
  u1s02 = slice13(u1s0,k2)
  u1s0 = None
  #plotLags23(Array.mul(4,u1s01),4.5,"u1s01.png")
  plotLags13(Array.mul(4,u1s02),4.5,"u1s02.png")

  u2s0 = readFloats3("u2s0.dat")
  u2s01 = slice23(u2s0,k1)
  u2s02 = slice13(u2s0,k2)
  u2s0 = None
  #plotLags23(Array.mul(25,u2s01),8.5,"u2s01.png")
  plotLags13(Array.mul(25,u2s02),8.5,"u2s02.png")

  u3s0 = readFloats3("u3s0.dat")
  u3s01 = slice23(u3s0,k1)
  u3s02 = slice13(u3s0,k2)
  u3s0 = None
  #plotLags23(Array.mul(25,u3s01),8.5,"u3s01.png")
  plotLags13(Array.mul(25,u3s02),8.5,"u3s02.png")

  u1s1 = readFloats3("u1s1.dat")
  u1s11 = slice23(u1s1,k1)
  u1s12 = slice13(u1s1,k2)
  u1s1 = None
  #plotLags23(Array.mul(4,u1s11),4.5,"u1s11.png")
  plotLags13(Array.mul(4,u1s12),4.5,"u1s12.png")

  u2s1 = readFloats3("u2s1.dat")
  u2s11 = slice23(u2s1,k1)
  u2s12 = slice13(u2s1,k2)
  u2s1 = None
  #plotLags23(Array.mul(25,u2s11),8.5,"u2s11.png")
  plotLags13(Array.mul(25,u2s12),8.5,"u2s12.png")

  u3s1 = readFloats3("u3s1.dat")
  u3s11 = slice23(u3s1,k1)
  u3s12 = slice13(u3s1,k2)
  u3s1 = None
  #plotLags23(Array.mul(25,u3s11),8.5,"u3s11.png")
  plotLags13(Array.mul(25,u3s12),8.5,"u3s12.png")

def main(args):
  plotSlices()
  return

# i2=288 is bin 1892 (288 = (1892-1316)/2)
# i2=267 is bin 1850 (267 = (1850-1316)/2)
#plotU1U3(288)
#plotU123(288)
#plotU1U3(267)
#plotU123(267)

#subtract()

#findAndRefine1()
#correct1()

#findAndRefine13()
#correct3()

#find3()
#refine3()
#plotS12(200)
#plotL12(200)

#i2a = 300
#plotS13(i2a)
#plotU13(i2a)

#i3a = 75
#plotS12(i3a)
#plotU12(i3a)

#i3b = 100
#plotS12(i3b)
#plotU12(i3b)

#i1a = 140
#plotS23(i1a)
#plotU23(i1a)

#i1b = 340
#plotS23(i1b)
#plotU23(i1b)

#############################################################################
# Do everything on Swing thread.

class RunMain(Runnable):
  def run(self):
    main(sys.argv)
SwingUtilities.invokeLater(RunMain())