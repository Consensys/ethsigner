
use warnings;
use strict;
use File::Find;

find (\&check_file, ".");

sub check_file
{
    my $file = $_;
    if ($file =~ /.*java$/ ) 
    {
	open(FH, '<', $file) or die $!;
	while (<FH>) 
        {
	   my $line = $_;
           if ($line =~ /.*\((.*)\).*\{/ and $line !~ /if |for /) 
           {
               my $bp = $1;
               if ($bp !~ /Exception/) 
               {
                   my @args = split /,/, $bp;
                   foreach my $arg (@args)
                   {
                       print "$file: $line" if($arg !~ /final/);
                   }
               }
           }
        } 
    }
}




