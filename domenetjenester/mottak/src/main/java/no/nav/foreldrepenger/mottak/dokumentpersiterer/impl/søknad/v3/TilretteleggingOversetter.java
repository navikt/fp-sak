package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Frilanser;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.SelvstendigNæringsdrivende;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Tilrettelegging;

public class TilretteleggingOversetter {

    private final VirksomhetTjeneste virksomhetTjeneste;
    private final PersoninfoAdapter personinfoAdapter;
    private final SvangerskapspengerRepository svangerskapspengerRepository;

    public TilretteleggingOversetter(SvangerskapspengerRepository svangerskapspengerRepository,
                                     VirksomhetTjeneste virksomhetTjeneste,
                                     PersoninfoAdapter personinfoAdapter) {
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
    }

    void oversettOgLagreTilretteleggingOgVurderEksisterende(Svangerskapspenger svangerskapspenger,
                                                            Behandling behandling, LocalDate søknadMottattDato) {
        var brukMottattTidspunkt = Optional.ofNullable(søknadMottattDato)
            .filter(d -> !d.equals(behandling.getOpprettetTidspunkt().toLocalDate()))
            .map(LocalDate::atStartOfDay)
            .orElseGet(behandling::getOpprettetTidspunkt);
        var svpBuilder = new SvpGrunnlagEntitet.Builder().medBehandlingId(behandling.getId());
        List<SvpTilretteleggingEntitet> nyeTilrettelegginger = new ArrayList<>();

        var tilretteleggingListe = svangerskapspenger.getTilretteleggingListe().getTilrettelegging();

        for (var tilrettelegging : tilretteleggingListe) {
            var builder = new SvpTilretteleggingEntitet.Builder();
            builder.medBehovForTilretteleggingFom(tilrettelegging.getBehovForTilretteleggingFom())
                .medKopiertFraTidligereBehandling(false)
                .medMottattTidspunkt(brukMottattTidspunkt)
                .medAvklarteOpphold(mapAvtaltFerie(tilrettelegging, svangerskapspenger));

            tilrettelegging.getHelTilrettelegging()
                .forEach(helTilrettelegging -> builder.medHelTilrettelegging(helTilrettelegging.getTilrettelagtArbeidFom(),
                    brukMottattTidspunkt.toLocalDate(), SvpTilretteleggingFomKilde.SØKNAD));

            tilrettelegging.getDelvisTilrettelegging()
                .forEach(delvisTilrettelegging -> builder.medDelvisTilrettelegging(delvisTilrettelegging.getTilrettelagtArbeidFom(),
                    delvisTilrettelegging.getStillingsprosent(), brukMottattTidspunkt.toLocalDate(), SvpTilretteleggingFomKilde.SØKNAD));

            tilrettelegging.getIngenTilrettelegging()
                .forEach(ingenTilrettelegging -> builder.medIngenTilrettelegging(ingenTilrettelegging.getSlutteArbeidFom(),
                    brukMottattTidspunkt.toLocalDate(), SvpTilretteleggingFomKilde.SØKNAD));

            oversettArbeidsforhold(builder, tilrettelegging.getArbeidsforhold());
            nyeTilrettelegginger.add(builder.build());
        }

        //I mangel på endringssøknad forsøker vi å flette eventuelle eksisterende tilrettelegginger med nye hvis mulig
        var eksisterendeTilrettelegginger = svangerskapspengerRepository.hentGrunnlag(behandling.getId())
            .map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElse(Collections.emptyList());
        List<SvpTilretteleggingEntitet> nyeOgEksisterendeTilrettelegginger = new ArrayList<>();

        if (!eksisterendeTilrettelegginger.isEmpty()) {
            var inputEksisterendeTilretteleggingMap = eksisterendeTilrettelegginger.stream().collect(Collectors.groupingBy(this::tilretteleggingNøkkel)); // Kortlevet
            var inputNyeTilretteleggingMap = nyeTilrettelegginger.stream().collect(Collectors.groupingBy(this::tilretteleggingNøkkel)); // Kortlevet

            var heltNyeMap = nyeTilrettelegginger.stream()
                .filter(nt -> inputEksisterendeTilretteleggingMap.get(tilretteleggingNøkkel(nt)) == null)
                .collect(Collectors.groupingBy(this::tilretteleggingNøkkel));

            heltNyeMap.forEach((key, value) -> nyeOgEksisterendeTilrettelegginger.addAll(value));

            var bareGamleMap = eksisterendeTilrettelegginger.stream()
                .filter(gt -> inputNyeTilretteleggingMap.get(tilretteleggingNøkkel(gt)) == null)
                .collect(Collectors.groupingBy(this::tilretteleggingNøkkel));

            bareGamleMap.forEach((key, value) -> value.forEach(tlr -> {
                var eksisterendeTilR = new SvpTilretteleggingEntitet.Builder(tlr).medKopiertFraTidligereBehandling(true).build();
                nyeOgEksisterendeTilrettelegginger.add(eksisterendeTilR);
            }));

            var fletteNyeMap = nyeTilrettelegginger.stream()
                .filter(nt -> inputEksisterendeTilretteleggingMap.get(tilretteleggingNøkkel(nt)) != null)
                .collect(Collectors.toMap(this::tilretteleggingNøkkel, Function.identity()));

            var fletteGamleMap = eksisterendeTilrettelegginger.stream()
                .filter(nt -> inputNyeTilretteleggingMap.get(tilretteleggingNøkkel(nt)) != null)
                .collect(Collectors.groupingBy(this::tilretteleggingNøkkel));

            for (var eksTilrettelegging : fletteGamleMap.entrySet()) {
                var nyTilrettelegging = fletteNyeMap.get(eksTilrettelegging.getKey());
                eksTilrettelegging.getValue().forEach(eksTlr -> nyeOgEksisterendeTilrettelegginger.add(
                    oppdaterEksisterendeTlrMedNyeFomsOgOpphold(nyTilrettelegging, eksTlr)));
            }
        } else {
            //ingen eksisterende
            nyeOgEksisterendeTilrettelegginger.addAll(nyeTilrettelegginger);
        }

        var svpGrunnlag = svpBuilder.medOpprinneligeTilrettelegginger(nyeOgEksisterendeTilrettelegginger).build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }

    private String tilretteleggingNøkkel(SvpTilretteleggingEntitet tilrettelegging) {
        return tilrettelegging.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElseGet(() -> tilrettelegging.getArbeidType().getKode());
    }

    SvpTilretteleggingEntitet oppdaterEksisterendeTlrMedNyeFomsOgOpphold(SvpTilretteleggingEntitet nyTlR,
                                                                                 SvpTilretteleggingEntitet eksisterendeTlr) {
        var nyFomListe = new ArrayList<>(nyTlR.getTilretteleggingFOMListe());
        var tidligsteNyFom = nyFomListe.stream().map(TilretteleggingFOM::getFomDato).min(LocalDate::compareTo).orElse(LocalDate.EPOCH);
        var eksisterendeFOMSomSkalBeholdes =  eksisterendeTlr.getTilretteleggingFOMListe().stream().filter(f -> f.getFomDato().isBefore(tidligsteNyFom)).toList();

        //Vi beholder innvilgede svangerskapspengeperioder som er før nyeste svangerskapspengeperiode
        eksisterendeFOMSomSkalBeholdes.forEach(eksFom ->
            nyFomListe.add(new TilretteleggingFOM.Builder()
                .fraEksisterende(eksFom)
                .medTidligstMottattDato(Optional.ofNullable(eksFom.getTidligstMotattDato())
                    .orElseGet(() -> eksisterendeTlr.getMottattTidspunkt().toLocalDate()))
                    .medKilde(SvpTilretteleggingFomKilde.TIDLIGERE_VEDTAK)
                .build()));

        nyFomListe.sort(Comparator.comparing(TilretteleggingFOM::getFomDato));

        //Vi beholder innvilgede oppholdsperioder som er før nyeste svangerskapspengeperiode
        var eksisterendeOppholdSomSkalBeholdes = eksisterendeTlr.getAvklarteOpphold().stream()
            .filter(eksOpphold -> eksOpphold.getFom().isBefore(tidligsteNyFom))
            .map(eksOpphold ->
                    SvpAvklartOpphold.Builder.nytt()
                        .medKilde(SvpOppholdKilde.TIDLIGERE_VEDTAK)
                        .medOppholdÅrsak(eksOpphold.getOppholdÅrsak())
                        .medOppholdPeriode(eksOpphold.getFom(), eksOpphold.getTom())
                        .build())
            .toList();

        var justertTilrettelegging = SvpTilretteleggingEntitet.Builder.fraEksisterende(eksisterendeTlr)
            .medBehovForTilretteleggingFom(nyFomListe.stream().map(TilretteleggingFOM::getFomDato).min(LocalDate::compareTo).orElse(null))
            .medTilretteleggingFraDatoer(nyFomListe)
            .medAvklarteOpphold(eksisterendeOppholdSomSkalBeholdes);
        //legger også til eventuelle nye oppholdsperioder
        nyTlR.getAvklarteOpphold().forEach(justertTilrettelegging::medAvklartOpphold);

        return justertTilrettelegging.build();
    }

    private void oversettArbeidsforhold(SvpTilretteleggingEntitet.Builder builder, Arbeidsforhold arbeidsforhold) {
        if (arbeidsforhold instanceof no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsgiver arbeidsgiverType) {
            builder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
            Arbeidsgiver arbeidsgiver;
            if (arbeidsforhold instanceof no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Virksomhet virksomhetType) {
                var orgnr = virksomhetType.getIdentifikator();
                virksomhetTjeneste.hentOrganisasjon(orgnr);
                arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
            } else {
                var arbeidsgiverIdent = new PersonIdent(arbeidsgiverType.getIdentifikator());
                var aktørIdArbeidsgiver = personinfoAdapter.hentAktørForFnr(arbeidsgiverIdent);
                if (aktørIdArbeidsgiver.isEmpty()) {
                    throw new TekniskException("FP-545381",
                        "Fant ikke personident for arbeidsgiver som er privatperson i PDL");
                }
                arbeidsgiver = Arbeidsgiver.person(aktørIdArbeidsgiver.get());
            }
            builder.medArbeidsgiver(arbeidsgiver);
        } else if (arbeidsforhold instanceof Frilanser frilanser) {
            builder.medArbeidType(ArbeidType.FRILANSER);
            builder.medOpplysningerOmRisikofaktorer(frilanser.getOpplysningerOmRisikofaktorer());
            builder.medOpplysningerOmTilretteleggingstiltak(
                ((Frilanser) arbeidsforhold).getOpplysningerOmTilretteleggingstiltak());
        } else if (arbeidsforhold instanceof SelvstendigNæringsdrivende selvstendig) {
            builder.medArbeidType(ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
            builder.medOpplysningerOmTilretteleggingstiltak(selvstendig.getOpplysningerOmTilretteleggingstiltak());
            builder.medOpplysningerOmRisikofaktorer(selvstendig.getOpplysningerOmRisikofaktorer());
        } else {
            throw new TekniskException("FP-187531", "Ukjent type arbeidsforhold i svangerskapspengesøknad");
        }
    }

    private static List<SvpAvklartOpphold> mapAvtaltFerie(Tilrettelegging tilrettelegging, Svangerskapspenger svp) {
        if (svp.getAvtaltFerieListe() == null || !(tilrettelegging.getArbeidsforhold() instanceof no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsgiver arbeidsgiver)) {
            return List.of();
        }

        var identifikator = arbeidsgiver.getIdentifikator();
        var avtalteFerier = svp.getAvtaltFerieListe().getAvtaltFerie();
        return avtalteFerier.stream()
            .filter(af -> Objects.equals(identifikator, af.getArbeidsgiver().getIdentifikator()))
            .map(ao -> SvpAvklartOpphold.Builder.nytt()
                .medKilde(SvpOppholdKilde.SØKNAD)
                .medOppholdÅrsak(SvpOppholdÅrsak.FERIE)
                .medOppholdPeriode(ao.getAvtaltFerieFom(), ao.getAvtaltFerieTom())
                .build())
            .toList();
    }

}
