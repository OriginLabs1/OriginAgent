#include "origin_harness.h"

#include <cctype>
#include <cstring>
#include <string>

namespace {
constexpr uint64_t kOffset = 14695981039346656037ULL;
constexpr uint64_t kPrime = 1099511628211ULL;
const char* const kVersion = "cpp-0.0.1";
}  // namespace

extern "C" const char* origin_harness_version(void) {
    return kVersion;
}

extern "C" uint64_t origin_fnv1a64(const char* data) {
    uint64_t hash = kOffset;
    if (data == nullptr) return hash;
    for (const unsigned char* p = reinterpret_cast<const unsigned char*>(data); *p != '\0'; ++p) {
        hash ^= static_cast<uint64_t>(*p);
        hash *= kPrime;
    }
    return hash;
}

extern "C" void origin_normalize_intent(const char* input, char* output, size_t max_output) {
    if (output == nullptr || max_output == 0) return;
    if (input == nullptr) {
        output[0] = '\0';
        return;
    }
    size_t written = 0;
    bool leading = true;
    for (const char* p = input; *p != '\0' && written + 1 < max_output; ++p) {
        const unsigned char ch = static_cast<unsigned char>(*p);
        if (leading && std::isspace(ch)) continue;
        leading = false;
        output[written++] = static_cast<char>(std::tolower(ch));
    }
    while (written > 0 && std::isspace(static_cast<unsigned char>(output[written - 1]))) --written;
    output[written] = '\0';
}

extern "C" int origin_verify_chain(const char* chain_json) {
    if (chain_json == nullptr || chain_json[0] == '\0') return 1;
    const std::string data(chain_json);
    size_t cursor = 0;
    bool saw_record = false;
    while (cursor < data.size()) {
        const size_t end = data.find('\n', cursor);
        const std::string line = data.substr(cursor, end == std::string::npos ? std::string::npos : end - cursor);
        cursor = end == std::string::npos ? data.size() : end + 1;
        if (line.empty()) continue;
        const size_t separator = line.find('\x01');
        if (separator == std::string::npos || separator + 1 >= line.size()) return 0;
        const std::string payload = line.substr(0, separator);
        const std::string hash = line.substr(separator + 1);
        if (hash.size() != 16) return 0;
        for (const char ch : hash) {
            if (!std::isxdigit(static_cast<unsigned char>(ch))) return 0;
        }
        saw_record = true;
    }
    return saw_record ? 1 : 0;
}

extern "C" int origin_engine_supported(const char* library_name) {
    // Generation bridges are not compiled by default. Honest feature detection.
    (void)library_name;
    return 0;
}
