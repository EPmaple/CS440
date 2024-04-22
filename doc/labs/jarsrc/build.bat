if exist build\ (
    rmdir build /s /q
)

mkdir build
javac -cp ./../lib/*;. -d build @jar.srcs


cd build
jar cvf ./../../lib/lab14.jar edu/
cd ..

rmdir build /s /q
