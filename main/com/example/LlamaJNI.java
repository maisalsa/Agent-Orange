// com/example/LlamaJNI.java
package com.example;

public class LlamaJNI {

    static {
        System.loadLibrary("llama_jni"); // or "llama_jni"
    }

    public native String generateResponse(String prompt);

    public static void main(String[] args) {
        LlamaJNI llama = new LlamaJNI();
        String userPrompt = "Write a short poem about cats.";
        String response = llama.generateResponse(userPrompt);
        System.out.println(response);
    }
}
