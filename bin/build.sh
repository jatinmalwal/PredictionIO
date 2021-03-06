#!/usr/bin/env sh

# PredictionIO Build Script

set -e

# Get the absolute path of the build script
SCRIPT="$0"
while [ -h "$SCRIPT" ] ; do
    SCRIPT=`readlink "$SCRIPT"`
done

# Get the base directory of the repo
DIR=`dirname $SCRIPT`/..
cd $DIR
BASE=`pwd`

. "$BASE/bin/common.sh"
. "$BASE/bin/vendors.sh"

# Full rebuild?
if test "$REBUILD" = "1" ; then
    echo "Rebuild set."
    CLEAN=clean
else
    echo "Incremental build set. Use \"REBUILD=1 $0\" for clean rebuild."
    CLEAN=
fi

echo "Going to build PredictionIO..."
BASE_TARGETS="update compile commons/publish output/publish"

if test "$SKIP_PROCESS" = "1" ; then
    echo "Skip building process assemblies."
else
    echo "+ Assemble Process Hadoop Scalding"
    BASE_TARGETS="$BASE_TARGETS processHadoopScalding/assembly"

    echo "+ Assemble Process Commons Evaluations Scala Parameter Generator"
    BASE_TARGETS="$BASE_TARGETS processEnginesCommonsEvalScalaParamGen/assembly"

    echo "+ Assemble Process Commons Evaluations Scala U2I Training-Test Splitter"
    BASE_TARGETS="$BASE_TARGETS processEnginesCommonsEvalScalaU2ITrainingTestSplit/assembly"

    echo "+ Assemble Process ItemRec Algorithms Scala Mahout"
    BASE_TARGETS="$BASE_TARGETS processEnginesItemRecAlgoScalaMahout/assembly"

    echo "+ Assemble Process ItemRec Evaluations Scala Top-k Items Collector"
    BASE_TARGETS="$BASE_TARGETS processEnginesItemRecEvalScalaTopKItems/assembly"

    echo "+ Assemble Process ItemSim Evaluations Scala Top-k Items Collector"
    BASE_TARGETS="$BASE_TARGETS processEnginesItemSimEvalScalaTopKItems/assembly"
fi

# Build GraphChi Data Preparator and Model Constructor
echo "+ Pack GraphChi Data Preparator and Model Constructor"
BASE_TARGETS="$BASE_TARGETS processEnginesItemRecAlgoScalaGraphChi/pack"

# Build connection check tool
echo "+ Pack Connection Check Tool"
BASE_TARGETS="$BASE_TARGETS toolsConncheck/pack"

# Build settings initialization tool
echo "+ Pack Settings Initialization Tool"
BASE_TARGETS="$BASE_TARGETS toolsSettingsInit/pack"

# Build software manager
echo "+ Pack Software Manager"
BASE_TARGETS="$BASE_TARGETS toolsSoftwareManager/pack"

# Build user tool
echo "+ Pack User Tool"
BASE_TARGETS="$BASE_TARGETS toolsUsers/pack"

$SBT $CLEAN $BASE_TARGETS

# Build admin server
echo "Going to build PredictionIO Admin Server..."
cd $BASE/servers/admin
$PLAY $CLEAN update compile

# Build API server
echo "Going to build PredictionIO API Server..."
cd $BASE/servers/api
$PLAY $CLEAN update compile

# Build scheduler server
echo "Going to build PredictionIO Scheduler Server..."
cd $BASE/servers/scheduler
$PLAY $CLEAN update compile
