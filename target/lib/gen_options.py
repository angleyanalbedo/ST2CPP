"""Generate ec_options.h from ec_options.h.in with CMake defaults."""
import re, sys, os

DEFAULTS = {
    "EC_BUFSIZE":       "1518",
    "EC_MAXBUF":        "16",
    "EC_MAXEEPBITMAP":  "128",
    "EC_MAXEEPBUF":     "4096",
    "EC_LOGGROUPOFFSET":"16",
    "EC_MAXELIST":      "64",
    "EC_MAXNAME":       "40",
    "EC_MAXSLAVE":      "200",
    "EC_MAXGROUP":      "2",
    "EC_MAXIOSEGMENTS": "64",
    "EC_MAXMBX":        "1486",
    "EC_MBXPOOLSIZE":   "32",
    "EC_MAXEEPDO":      "0x200",
    "EC_MAXSM":         "8",
    "EC_MAXFMMU":       "4",
    "EC_MAXLEN_ADAPTERNAME": "128",
    "EC_MAX_MAPT":      "1",
    "EC_MAXODLIST":     "1024",
    "EC_MAXOELIST":     "256",
    "EC_SOE_MAXNAME":   "60",
    "EC_SOE_MAXMAPPING":"64",
    "EC_TIMEOUTRET":    "2000",
    "EC_TIMEOUTRET3":   "6000",
    "EC_TIMEOUTSAFE":   "20000",
    "EC_TIMEOUTEEP":    "20000",
    "EC_TIMEOUTTXM":    "20000",
    "EC_TIMEOUTRXM":    "700000",
    "EC_TIMEOUTSTATE":  "2000000",
    "EC_DEFAULTRETRIES":"3",
    "EC_PRIMARY_MAC_ARRAY":   "{0x0101, 0x0101, 0x0101}",
    "EC_SECONDARY_MAC_ARRAY": "{0x0404, 0x0404, 0x0404}",
}

def main():
    src = sys.argv[1] if len(sys.argv) > 1 else None
    dst = sys.argv[2] if len(sys.argv) > 2 else None

    if not src:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        src = os.path.join(script_dir, "soem", "include", "soem", "ec_options.h.in")
    if not dst:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        dst = os.path.join(script_dir, "soem", "include", "soem", "ec_options.h")

    with open(src, "r") as f:
        content = f.read()

    def repl(m):
        key = m.group(1)
        return DEFAULTS.get(key, m.group(0))

    content = re.sub(r"@(\w+)@", repl, content)

    with open(dst, "w") as f:
        f.write(content)

    print(f"Generated: {dst}")

if __name__ == "__main__":
    main()
