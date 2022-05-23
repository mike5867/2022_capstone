from enum import Enum

class Result(Enum):
    DIFF=-2
    DETECTED_FAIL=-1
    PASS=1
    FAIL=0
