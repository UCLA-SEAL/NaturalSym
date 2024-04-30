echo deprecated script!
rm ../Rundir/*
#benchmark=pigmixl2/L2
#benchmark=wordcount/WordCount
#benchmark=incomeaggregate/IncomeAggregate
#benchmark=movieratings/MovieRatingsCount
#benchmark=airporttransit/AirportTransit
#benchmark=gradeanalysis/StudentGrades
benchmark=commutetype/CommuteType

pushd bin
java -ea udfExtractor.Runner ${benchmark}
popd