# JpkTaxApp

## Przeznaczenie

Projekt JpkTaxApp to prosta implementacji aplikacji biurowej w środowisku JAVA do obsługi dokumentów JPK wymaganych przez organy kontroli skarbowej oraz Ministerstwo Finansów zgodna ze specyfikacją zamieszczoną na stronie http://jpk.mf.gov.pl/ w sekcji "Specyfikacja interfejsów usług Jednolitego Pliku Kontrolnego" w wersji 1.7

## Wymagania

W celu uruchomienia modułów projektu wymagane jest zainstalowanie środowiska uruchomieniowego JAVA w wersji 8 (JRE 8) lub wyższej dostępnego pod adresem:
https://www.java.com/pl/download/

## Instalacja
 
Moduły dostarczone są w formie archiwum ZIP, a proces instalacji polega na wypakowaniu katalogu w dowolne miejsce na lokalnym dysku. W celu uruchomienia w  katalogu aplikacji przygotowane są skrypty systemu MS Windows application.bat dla aplikacji okienkowej oraz console.bat dla aplikacji konsolowej.

## UWAGA

Do uruchomienia aplikacji JpkInitApp wymagane jest uzupełnienie środowiska uruchomieniowego JAVA o biblioteki obsługujące szyfrowanie algorytmem AES256. W tym celu należy przekopiować z uprawnieniami administratora biblioteki local_policy.jar oraz US_export_policy.jar znajdujące się w katalogu bibliotek aplikacji JpkInitApp\lib\security do katalogu bibliotek używanego JRE, dla przykładu w JRE w wersji 1.8.0_101MS Windows:

c:\Program Files\Java\jre1.8.0_101\lib\security

c:\Program Files (x86)\Java\jre1.8.0_101\lib\security

oczywiście w systemach Linux'owo podobnych katalog JRE może być dowolny, na przykład:

/usr/java/jre1.8.0_101/lib/security

## Kontak:
jpktaxapp@gmail.com
