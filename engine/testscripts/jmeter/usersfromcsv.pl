#!/usr/bin/perl

# takes a file path to a csv file as an argument and then adds the users from that file to nakamura
$clean=100;
$counter_time=0;
$counter=0;
$first=time;

$file = $ARGV[0];
$host = $ARGV[1];
$port = $ARGV[2];

open (F, $file) || die ("Could not open $file!");

while ($line = <F>)
{
  ($name,$password,$firstName,$lastName) = split ',', $line;
  chomp($lastName);
  if ($counter_time==$clean) {
      $last=time;
      $diff=$last-$first;
      print "$counter,$diff\n";
      $first=$last;
      $counter_time=0;
  }
  $counter_time++;
  $counter++;
  $email="$name@sakai.invalid";
  $val=&create_profile($name,$firstName,$lastName,$email); 
  system ("curl $val -F:name=$name -Fpwd=test -FpwdConfirm=test -F:sakai:pages-template=/var/templates/site/defaultuser -Femail=$email  http://admin:admin\@$host:$port/system/userManager/user.create.html 2> /dev/null  >/dev/null;");
}

close (F);

sub create_profile{
my ($tester,$firstName,$lastName,$email)=@_;  
  $ret= "-F \":sakai:profile-import={\'basic\':{\'elements\':{\'firstName\':{\'value\':\'$firstName\'},\'lastName\':{\'value\':\'$lastName\'},\'email\':{\'value\':\'$email\'}},\'access\':\'everybody\'}}\" ";
return $ret;
}



