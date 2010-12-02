#!/usr/bin/perl

# Ugly hack to test how long it costs to create a certain number of users by curl
# a.m.berg@uva.nl

$users=2000;
$clean=100;
$counter_time=0;
$counter=0;
$first=time;
while ($counter < $users){
    if ($counter_time==$clean) {
        $last=time;
        $diff=$last-$first;
        print "$counter,$diff\n";
        $first=$last;
        $counter_time=0;
    }
    $counter_time++;
    $counter++;
    $val=&create_profile("tester$counter"); 
    system ("curl $val -F:name=tester$counter -Fpwd=test -FpwdConfirm=test http://admin:admin\@localhost:8080/system/userManager/user.create.html 2> /dev/null  >/dev/null;");
}


sub create_profile{
my ($tester)=@_;  
  $ret=  "-F \":sakai:profile-import={\'basic\': {\'access\': \'everybody\', \'elements\': {\'email\': {\'value\': \'$tester\@sakai.invalid\'}, \'firstName\': {\'value\': '$tester'}, 'lastName': {'value': \'$tester\'}}}}\"";
return $ret;
}



