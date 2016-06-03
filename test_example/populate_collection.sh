echo "uploading data.txt to game_of_thrones in elasticsearch"
curl -s -XPOST localhost:9200/_bulk?pretty --data-binary "@data.txt"; echo
