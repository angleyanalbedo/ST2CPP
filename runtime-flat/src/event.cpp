#include "core/event.h"

namespace rt_plc {

bool Event::check(GVL& gvl, ProcessImage& io) {
    BOOL currentState = condition ? condition(gvl, io) : FALSE;
    bool triggered = false;
    switch (edge) {
        case EventEdge::RISING:  triggered = (currentState && !lastState); break;
        case EventEdge::FALLING: triggered = (!currentState && lastState); break;
        case EventEdge::BOTH:    triggered = (currentState != lastState);  break;
    }
    lastState = currentState;
    return triggered;
}

} // namespace rt_plc
