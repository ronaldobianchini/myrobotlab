# a minimal tracking script - this will start all peer
# setXMinMax & setYMinMax (min, max)
# this will set the min and maximum
# values of the servos 
tracker.setXMinMax(10, 170)
tracker.setYMinMax(10, 170)

# set rest x,y
tracker.setServoPins(13,12)
#change cameras if necessary
tracker.setCameraIndex(1)
# start the tracking service
# set a point and track it
# where 0.5,0.5 is middle of screen
tracker.trackPoint(0.5, 0.5)
# don't be surprised if the point does not
# stay - it needs / wants a corner in the image
# to presist - otherwise it might disappear
# you can set points manually by clicking on the
# opencv screen