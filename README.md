在main中调整input-prefix，会生成相应的三个txt文件：-context.txt, -node.txt, -path.txt。
其中context的格式如下：

    method_name:[NAME]
    score:[SCORE]
    start1,path1,end1
    ...
    method_name:[NAME]
    score:[SCORE]
    start33,path33,end33
    ...

-node.txt和-path.txt为各个node和path的编码，格式为：
    
    index,content
    ...