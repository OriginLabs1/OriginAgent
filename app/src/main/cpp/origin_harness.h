#ifndef ORIGIN_HARNESS_H
#define ORIGIN_HARNESS_H

#include <cstddef>
#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif

const char* origin_harness_version(void);
uint64_t origin_fnv1a64(const char* data);
void origin_normalize_intent(const char* input, char* output, size_t max_output);
int origin_verify_chain(const char* chain_json);
int origin_engine_supported(const char* library_name);

#ifdef __cplusplus
}
#endif

#endif // ORIGIN_HARNESS_H
