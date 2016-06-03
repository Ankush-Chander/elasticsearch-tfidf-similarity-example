curl -XDELETE 'http://localhost:9200/hello_world_example/'

curl -XPUT 'localhost:9200/hello_world_example' -d '
{
    "settings" : {
        "index" : {
            "number_of_shards" : 1,
            "number_of_replicas" : 1,
            "store": "memory"
        },

        "similarity" : {
      	  "custom_similarity" : {
        		"type" : "tfidfsimilarity"
      	  }
        }
    }
}
'

curl -XGET 'http://localhost:9200/hello_world_example/_settings?pretty'


curl -XPUT 'localhost:9200/hello_world_example/_mapping/pages' -d '
{
  "properties": {
    "text": {
      "type": "string",
      "analyzer": "standard",
      "index_options": "positions"
    }
  }
}
'
