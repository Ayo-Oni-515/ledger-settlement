java -XX:+PrintFlagsFinal -version | grep -E 'MaxHeapSize|UseG1GC'
   size_t MaxHeapSize                              = 4219469824                                {product} {ergonomic}
   size_t SoftMaxHeapSize                          = 4219469824                             {manageable} {ergonomic}
     bool UseG1GC                                  = true                                      {product} {ergonomic}
java version "25.0.4.1" 2026-08-18 LTS
Java(TM) SE Runtime Environment (build 25.0.4.1+1-LTS-5)
Java HotSpot(TM) 64-Bit Server VM (build 25.0.4.1+1-LTS-5, mixed mode, sharing)


