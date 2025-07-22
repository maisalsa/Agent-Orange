// llama_jni.cpp
#include <iostream>
#include <string> // For std::string
#include "com_example_LlamaJNI.h" // Replace with your actual package and class name
#include "llama.h" // Include llama.cpp headers

extern "C" {
    JNIEXPORT jstring JNICALL Java_com_example_LlamaJNI_generateResponse(JNIEnv *env, jobject obj, jstring prompt) {
        // 1. Convert Java string to C++ string
        const char *prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
        std::string cpp_prompt(prompt_cstr);
        env->ReleaseStringUTFChars(prompt, prompt_cstr); // Release the memory

        // 2. Load the Llama model (this should ideally be done only once at initialization)
        // (Adapt the code from the article to load the model)
        struct llama_model *model = llama_load_model_from_file("path/to/your/model.gguf", llama_context_default_params());
        if (!model) {
            std::cerr << "Failed to load model\n";
            return env->NewStringUTF("Error: Failed to load model");
        }

        // 3. Create a llama_context
        struct llama_context *ctx = llama_new_context_with_model(model, llama_context_default_params());

        // 4. Format prompt with system message.
        std::string systemMessage = "You are a helpful assistant. Respond concisely.";
        std::string finalPrompt = systemMessage + cpp_prompt;

        // 5. Perform Inference (Adapt the code from the article to generate text)
        std::string generated_text = "";
        {
            // Tokenize the prompt
            std::vector<llama_token> tokens = llama_tokenize(ctx, finalPrompt, true, true);

            // Evaluate the prompt
            llama_eval(ctx, tokens.data(), tokens.size(), 0);

            // Generate tokens until the end of sequence token is found
            llama_token token = llama_sample(ctx, NULL, NULL, 0);
            while (token != llama_token_eos(ctx)) {
                generated_text += llama_token_to_str(ctx, token);

                llama_eval(ctx, &token, 1, llama_n_tokens(ctx));

                token = llama_sample(ctx, NULL, NULL, 0);
            }
        }

        llama_free_context(ctx);
        llama_free_model(model);

        // 6. Convert the C++ string to a Java string
        jstring result = env->NewStringUTF(generated_text.c_str());

        return result;
    }
}
