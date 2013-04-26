#Needs PIL, matplotlib, pgu, VideoCapture, pygame, and numpy

import PIL
import ImageStat
import matplotlib.pyplot as plt
from time import time
from threading import Thread
from time import sleep
from pgu import gui
import NoFontVideoCapture
from Image import new
from ImageOps import grayscale
import pygame

fps = 15.0
dur = 1.0/fps;
defMin = 0;
defMax = 100;
autoScale = False;
period = 10;
minValue = defMin;
maxValue = defMax;
myMode = 1;
maskIm = False;
cam = False;
getImage = False;
width = 640;
height = 480;
res = (width,height);
leniency = 5;
windowClosed = False;
finished = False;

def startProcess():
  setup(width,height);
  if not maskIm:
    return;
#  screen = pygame.display.set_mode(res)
#  pygame.display.set_caption('Webcam')
  divVal = ImageStat.Stat(maskIm).sum[0]/100.0;
  fig = plt.figure()
  ax = fig.add_subplot(111)
  line, = ax.plot([], [], animated=True, lw=2)
  halfPer = period/2;
  ax.set_xlim(0, period)
  ax.set_ylim(minValue,maxValue);
  ax.grid()
  xdata, ydata = [], []
  def run(*args):
    # for profiling
    tstart = time()
    Thread(target = loop,args = (tstart,)).start();
#    loop(tstart);

  def loop(tstart):
    xmin = 0;
    xmax = period;
    global minValue, maxValue;
    if autoScale:
      minValue = False;
      maxValue = False;
    background = fig.canvas.copy_from_bbox(ax.bbox)
    myTime = tstart;
    while manager.window!=None:
      colorcamshot = getImage();
      camshot = grayscale(colorcamshot);
      brightness = ImageStat.Stat(camshot,maskIm).sum[0]/divVal;
#      camshot = pygame.image.frombuffer(colorcamshot.tostring(), res, "RGB")
#      screen.blit(camshot, (0,0))
#      pygame.display.flip()
      lastTime = myTime;
      myTime = time();
      timeDif = myTime-lastTime;
      if timeDif<dur:
         sleep(dur-(myTime-lastTime));
      # update the data
      t = myTime-tstart
      xdata.append(t)
      ydata.append(brightness)
      if not maxValue:
        minValue = maxValue = brightness;
      elif t>=xmax or (autoScale and (brightness<minValue or brightness>maxValue)):
        if myMode==2 and t>=xmax:
          break;
        else:
          fig.canvas.restore_region(background)
          if autoScale and t < xmax:
            minValue = min(minValue,brightness);
            maxValue = max(maxValue,brightness);
            add=(maxValue-minValue)/8;
            ax.set_ylim(max(minValue-add,0),min(maxValue+add,100));
          else:
            xmin += halfPer;
            cutoff = binSearch(xdata,xmin);
            xmax += halfPer;
            del xdata[:cutoff]
            del ydata[:cutoff]
            ax.set_xlim(xmin,xmax);
            if autoScale:
              minValue = min(ydata);
              maxValue = max(ydata);
              add = (maxValue-minValue)/8;
              ax.set_ylim(max(minValue-add,0),min(maxValue+add,100));
          if windowClosed:
            break;
#          try:
          fig.canvas.draw()
#          except Exception:
#            break;
          background = fig.canvas.copy_from_bbox(ax.bbox)
          line.set_data(xdata,ydata);
      else:
        line.set_data(xdata[-2:], ydata[-2:])
      if windowClosed:
        break;
      # just draw the animated artist
#      try:
      ax.draw_artist(line)
#      except Exception:
#        break;
      if windowClosed:
        break;
      # just redraw the axes rectangle
#      try:
      fig.canvas.blit(ax.bbox)
#      except Exception:
#        break;
      # TODO Add Experiment mode termination here
    if windowClosed:
      plt.close();
    else:
      global finished;
      finished = True;
#    pygame.quit();

  manager = plt.get_current_fig_manager()
  def handler():
    if finished:
      plt.close();
    else:
      global windowClosed;
      windowClosed = True;
  manager.window.protocol("WM_DELETE_WINDOW", handler)
  manager.window.after(1000, run)
  plt.show()

def setup(width,height):
  app = gui.Desktop()
  app.connect(gui.QUIT, app.quit, None)
  c = gui.Table(width=500, height=150)
  c.tr()
  c.td(gui.Label("Config"),colspan=6)
  c.tr()
  c.td(gui.Label(""))
  md = gui.Group(name='mode',value=1)
  fx = gui.Group(name='fix',value=1)
  per = gui.Input(name='period',value='10',size=4)
  mn = gui.Input(name='min',value=str(defMin),size=4)
  mx = gui.Input(name='max',value=str(defMax),size=4)
  c.tr()
  c.td(gui.Radio(md,value=1))
  c.td(gui.Label("Exhibit mode"),align=-1)
  c.tr()
  c.td(gui.Radio(md,value=2))
  c.td(gui.Label("Experiment mode"),align=-1)
  c.td(gui.Label("Period (secs)"),align=1)
  c.td(per)
  c.tr()
  c.td(gui.Label(""))
  c.tr()
  rButton = gui.Radio(fx,value=1);
  c.td(rButton)
  def disableButtons(boolVal):
    mn.disabled = mx.disabled = boolVal;
    if boolVal:
      mn.value = mx.value = "";
    else:
      mn.value = str(defMin);
      mx.value = str(defMax);
    mn.repaint();
    mx.repaint();
  rButton.connect(gui.CLICK, disableButtons, False); 
  c.td(gui.Label("Fixed scale"),align=-1)
  c.td(gui.Label("Min"),align=1)
  c.td(mn)
  c.td(gui.Label("Max"),align=1)
  c.td(mx)
  c.tr()
  rButton = gui.Radio(fx,value=2);
  c.td(rButton)
  rButton.connect(gui.CLICK, disableButtons, True); 
  c.td(gui.Label("Autoscale"),align=-1)  
  c.tr()
  c.td(gui.Label(""));
  starDetect = gui.Button(value="Detect Star");
  c.td(starDetect, align=-1);
  finishButton = gui.Button(value="OK");
  finishButton.disabled = True;
  finishButton.connect(gui.CLICK, app.quit, None);
  c.td(finishButton,align=0);
  def determineStar(oldWidget):
    c = gui.Table(width=500,height=100);
    c.tr();
    c.td(gui.Label("Turn on your star and position the camera"));
    c.tr();
    c.td(gui.Label("so the star is in view."));
    c.tr();
    c.td(gui.Label("Make sure no planets are in the way."));
    c.tr();
    c.td(gui.Label("Then press OK"));
    c.tr();
    rButton = gui.Button(value = "OK");
    c.td(rButton);
    c.tr();
    rButton.connect(gui.CLICK, app.quit, None)
    app.quit();
    app.run(c);
    app.remove(c);
    global cam;
    global getImage;
    if not cam:
      cam = NoFontVideoCapture.Device();
      def getImage():
        return cam.getImage();
    for i in range(20):
      getImage();
    camshot = grayscale(getImage());
    lightCoords = [];
    level = camshot.getextrema()[1]-leniency;
    for p in camshot.getdata():
      if p>=level:
        lightCoords.append(255);
      else:
        lightCoords.append(0);
    global maskIm;
    maskIm = new("L",res);
    maskIm.putdata(lightCoords);
    finishButton.disabled = False;
    app.run(oldWidget);
  starDetect.connect(gui.CLICK, determineStar, c);
  app.run(c)
  global autoScale;
  autoScale = (fx.value == 2);
  global myMode;
  myMode = md.value;
  global minValue, maxValue;
  if not autoScale:
    minValue = float(mn.value);
    maxValue = float(mx.value);
  global period;
  period = float(per.value);
  pygame.quit();


def binSearch(vals,val):
  low = 0;
  high = len(vals)+1;
  while high-low>1:
    guess = (low+high)>>1;
    if guess>=len(vals) or vals[guess]>val:
      high = guess;
    else:
      low = guess;
  return low;
startProcess();
