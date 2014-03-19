/*
******************************
PROCESS AND EVENT SCHEMAS 
DEFINED IN ELLEN'S DOC

Schemas found in this file:
-Process
-Action
-ForceTransfer
-ForceApplicationAction
-Motion
-MotorControlAction
-LocomotionAction
-WalkAction
-CarryAction

-Event
-MotionPathEvent
-SelfMotionPathEvent
-ControlProcessEvent
-ControlMotionEvent
-ControlMotionPathEvent
-CauseEffectEvent
-CauseMotionEvent
-CauseMotionPathEvent
******************************
*/

/*
******************************
PROCESSES AND ACTIONS

Schemas found in this section:
-Process
-Action
-ForceTransfer
-ForceApplicationAction
-Motion
-MotorControlAction
-LocomotionAction
-WalkAction
-CarryAction

******************************
*/

schema Process
  roles
    protagonist



schema Action
  subcase of Process
  roles
    actor
  constraints
    actor <--> protagonist



schema ForceTransfer
  subcase of Process
  roles
    supplier
    recipient
  constraints
    supplier <--> protagonist



schema ForceApplicationAction
  subcase of Action
  evokes ForceTransfer as forceTransfer
  roles
    actedUpon
  constraints
    //actor <--> protagonist
    forceTransfer.supplier <--> actor
    forceTransfer.recipient <--> actedUpon



schema Motion
  subcase of Process
  roles
    mover
    speed
    heading
  constraints
    mover <--> protagonist



schema MotorControlAction
  subcase of Action
  roles
    executor:Animate
    effector:BodyPart
    effort
    routine 
  constraints
    executor <--> actor
    effort <-- "positive"
    effector.owner <--> executor


    
schema LocomotionAction
  subcase of MotorControlAction, Motion
  roles
    selfmover
    gait
  constraints
    selfmover <--> executor
    selfmover <--> mover
    gait <--> routine



schema WalkAction
  subcase of LocomotionAction
  evokes Legs as l
  roles
    walker
  constraints
    walker <--> selfmover
    gait <--> self
    effector <--> l



schema CarryAction
  subcase of ForceApplicationAction
  evokes LocomotionAction as l
  constraints
    actor <--> l.selfmover
    


/*
******************************
EVENTS

Schemas found in this section:
-Event
-MotionPathEvent
-SelfMotionPathEvent
-ControlProcessEvent
-ControlMotionEvent
-ControlMotionPathEvent
-CauseEffectEvent
-CauseMotionEvent
-CauseMotionPathEvent

******************************
*/


schema Event
  roles
   profiled-participant
   profiled-process:Process



schema MotionPathEvent
  subcase of Event
  evokes SPG as spg
  roles
    mover
    landmark
    motion:Motion
  constraints
    mover <--> motion.mover
    mover <--> spg.trajector
    landmark <--> spg.landmark



schema SelfMotionPathEvent
  subcase of MotionPathEvent
  evokes LocomotionAction as la
  constraints
    mover <--> la.executor



schema ControlProcessEvent
  subcase of Event
  roles
    controller
    controlled
    controllingAction:Action
    controlMeans:ForceTransfer
    dependentProcess:Process
  constraints
    controller <--> controllingAction.actor
    controller <--> controlMeans.supplier
    controlled <--> controlMeans.recipient
    controlled <--> dependentProcess.protagonist



schema ControlMotionEvent
  subcase of ControlProcessEvent
  evokes Motion as motion
  constraints
     dependentProcess <--> motion
     controlled <--> motion.mover



schema ControlMotionPathEvent
  subcase of ControlMotionEvent, MotionPathEvent
  constraints
    controlled <--> mover


      
schema CauseEffectEvent
  subcase of Event
  roles
    causer
    affected
    causalAction:Action
    cause:ForceTransfer
    effect:Process
  constraints
    causer <--> causalAction.actor
    causer <--> cause.supplier
    affected <--> effect.protagonist
    affected <--> cause.recipient



schema CauseMotionEvent
  subcase of CauseEffectEvent
  evokes Motion as motion
  constraints
    effect <--> motion
    affected <--> motion.mover



schema CauseMotionPathEvent
  subcase of CauseMotionEvent, MotionPathEvent
  constraints
    affected <--> mover


