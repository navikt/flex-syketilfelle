# flex-syketilfelle

---

flex-syketilfelle brukes til å beregne arbeidsgiverperiode, ventepriode og et sykeforløp.
Sykeforløp brukes ofte til å hente første dagen i et syketilfelle

Appen kalles av sykepengesoknad-backend, sykmelding frontend og ditt sykefravær frontend.

## Inkommenda data
Data input til appen er topicene med søknader og sykmeldinger. Disse normaliseres til en syketilfellebit og lagres i databasen. Dette for å gjøre det enklere å lage logikk på en tidslinje av hendelser.


## Topic  
De samme bitene produserer også til topicet *flex.syketilfellebiter*.
Topicet har full historikk fra den gamle on-prem versjonen av denne appen, *syfosyketilfelle*.
Appen eier også et topic hvor datagrunnlaget deles videre til andre team i PO Helse som gjør andre beregninger på samme datagrunnlag.

Key er fødselsnummer. Fødselsnummeret bæres videre uendret fra søknaden og sykmeldingen. For detaljer på innholdet se filen *KafkaSyketilfellebit.kt*
Flere biter kan dele samme ressursId. F.eks. hvis en søknad har data om permisjon og ferie så vil dette være to biter, men 
Et eksempel på en bit ser slik ut.

```json
{
  "id":"27461cf0-b494-4dff-a42b-a64534fd1097",
  "fnr":"12345678987",
  "orgnummer":"org",
  "opprettet":"2022-01-03T08:32:02.417954+01:00",
  "inntruffet":"2022-01-03T08:32:02.417914+01:00",
  "tags":[
    "SENDT",
    "SYKEPENGESOKNAD"
  ],
  "ressursId":"146e8fef-92e4-4186-97ea-749672e7bf36",
  "fom":"2022-01-01",
  "tom":"2022-01-03",
  "korrigererSendtSoknad":"d9c4457c-f3fd-441f-bc7f-a00ba91136e1"
}
```


### Korrigerende søknad
Noen biter kan korrigere andre biter. Dette må konsumenten av topicet ta hensyn til. 
Dersom *korrigererSendtSoknad* er satt **må** konsumenten ignorere andre biter hvor ressursId er lik korrigererSendtSoknad.
Dette kan løses ved å fortsatt lagre ned alle biter, men filtrere ut de som er korrigert når man leser bitene til en person opp.


## Data
Applikasjonen har en database i GCP. Syketilfellebitene er normaliserte data basert på søknad og sykmelding. Dataene er personidentifiserbare. Det slettes ikke data fra tabellen.

Topicet som holder de samme dataene har evig reteniton.

# Komme i gang

Bygges med gradle. Standard spring boot oppsett.

---

# Henvendelser


Spørsmål knyttet til koden eller prosjektet kan stilles til flex@nav.no

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #flex.
