#ifndef LLAMA_H
#define LLAMA_H

#ifdef __cplusplus
extern "C" {
#endif

// Minimal llama.cpp interface for JNI
// This is a placeholder for demonstration purposes
// For full functionality, install complete llama.cpp

typedef struct llama_context llama_context;
typedef struct llama_model llama_model;

// Placeholder functions
struct llama_model * llama_load_model_from_file(const char * path_model, struct llama_context_params params);
void llama_free_model(struct llama_model * model);
struct llama_context * llama_new_context_with_model(struct llama_model * model, struct llama_context_params params);
void llama_free(struct llama_context * ctx);

#ifdef __cplusplus
}
#endif

#endif // LLAMA_H
