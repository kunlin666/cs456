import sys

port1 = sys.argv[1]
port2 = sys.argv[2]
port3 = sys.argv[3]
port4 = sys.argv[4]

a = './nEmulator-linux386 '+port1+' 129.97.167.43 '+ port4 + ' ' + port3 + ' 129.97.167.43 ' + port2 + ' 1 0 0'
b = 'java receiver 129.97.167.43 '+ port3 + ' '+port4 + ' b'
c = 'java sender 129.97.167.43 '+ port1 + ' '+port2 + ' a'

print a
print b
print c