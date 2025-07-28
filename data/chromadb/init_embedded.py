#!/usr/bin/env python3
"""
Initialize embedded ChromaDB for offline operation
"""
import chromadb
from chromadb.config import Settings
import os

def initialize_chromadb():
    """Initialize ChromaDB in embedded mode"""
    persist_dir = os.path.dirname(os.path.abspath(__file__)) + "/persist"
    
    client = chromadb.Client(Settings(
        chroma_db_impl="duckdb+parquet",
        persist_directory=persist_dir,
        anonymized_telemetry=False
    ))
    
    # Create a test collection to verify functionality
    try:
        collection = client.create_collection(
            name="test_offline_collection",
            metadata={"description": "Test collection for offline verification"}
        )
        
        # Add a test document
        collection.add(
            documents=["This is a test document for offline ChromaDB"],
            metadatas=[{"source": "offline_test"}],
            ids=["test_doc_1"]
        )
        
        # Query to verify
        results = collection.query(
            query_texts=["test document"],
            n_results=1
        )
        
        print("✅ ChromaDB offline initialization successful")
        print(f"✅ Test collection created with {len(results['documents'][0])} documents")
        
        return True
        
    except Exception as e:
        print(f"❌ ChromaDB initialization failed: {e}")
        return False

if __name__ == "__main__":
    initialize_chromadb()
