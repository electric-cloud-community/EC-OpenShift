use ElectricCommander;

push (@::gMatchers,
  {
        id =>          "getToken",
        pattern =>     q{"msg":\s"(.+)"},
        action =>           q{
         
                              my $service_token = ((defined $::gProperties{"service_token"}) ? $::gProperties{"service_token"} : '');
                              
                              setProperty("/myJob/service_token", $1);
                             },
  }
);