package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadDataFraTidligereVedtakTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.Endringssoeknad;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.MorsAktivitetsTyper;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Gradering;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.LukketPeriodeMedVedlegg;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Oppholdsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Overfoeringsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Person;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Utsettelsesperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Virksomhet;

public class ForeldrepengerUttakOversetter  {

    private final VirksomhetTjeneste virksomhetTjeneste;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final PersoninfoAdapter personinfoAdapter;
    private final SøknadDataFraTidligereVedtakTjeneste søknadDataFraTidligereVedtakTjeneste;

    public ForeldrepengerUttakOversetter(YtelsesFordelingRepository ytelsesFordelingRepository,
                                         VirksomhetTjeneste virksomhetTjeneste,
                                         PersoninfoAdapter personinfoAdapter,
                                         SøknadDataFraTidligereVedtakTjeneste søknadDataFraTidligereVedtakTjeneste) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.søknadDataFraTidligereVedtakTjeneste = søknadDataFraTidligereVedtakTjeneste;
    }


    void oversettForeldrepengerEndringssøknad(Endringssoeknad omYtelse,
                                                              Behandling behandling,
                                                              LocalDate mottattDato) {
        var fordeling = omYtelse.getFordeling();
        var perioder = fordeling.getPerioder();
        var annenForelderErInformert = fordeling.isAnnenForelderErInformert();
        var ønskerJustertVedFødsel = fordeling.isOenskerJustertVedFoedsel();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(lagOppgittFordeling(behandling, perioder, annenForelderErInformert, mottattDato, ønskerJustertVedFødsel));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
    }

    void oversettForeldrepengerSøknad(Foreldrepenger omYtelse,
                                             Behandling behandling,
                                              LocalDate søknadMottattDato) {
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(oversettFordeling(behandling, omYtelse, søknadMottattDato));
        if (!behandling.erRevurdering()) {
            yfBuilder.medOppgittDekningsgrad(oversettDekningsgrad(omYtelse))
                .medSakskompleksDekningsgrad(null);
        }
        oversettRettighet(omYtelse).ifPresent(yfBuilder::medOppgittRettighet);
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
    }

    private Optional<OppgittRettighetEntitet> oversettRettighet(Foreldrepenger omYtelse) {
        return Optional.ofNullable(omYtelse.getRettigheter())
            .map(rettigheter -> new OppgittRettighetEntitet(rettigheter.isHarAnnenForelderRett(), rettigheter.isHarAleneomsorgForBarnet(),
                    harOppgittUføreEllerPerioderMedAktivitetUføre(omYtelse, rettigheter.isHarMorUforetrygd()), rettigheter.isHarAnnenForelderTilsvarendeRettEOS(),
                rettigheter.isHarAnnenForelderOppholdtSegIEOS()));
    }

    // TODO: Avklare med AP om dette er rett måte å serve rettighet??? Info må uansett sjekke oppgitt fordeling for eldre tilfelle (med mindre vi kjører DB-oppdatering)
    private static boolean harOppgittUføreEllerPerioderMedAktivitetUføre(Foreldrepenger omYtelse, Boolean oppgittUføre) {
        return oppgittUføre != null && oppgittUføre || omYtelse.getFordeling()
            .getPerioder()
            .stream()
            .anyMatch(p -> p instanceof Uttaksperiode uttak && erPeriodeMedAktivitetUføre(uttak.getMorsAktivitetIPerioden())
                || p instanceof Utsettelsesperiode utsettelse && erPeriodeMedAktivitetUføre(utsettelse.getMorsAktivitetIPerioden()));
    }

    private static boolean erPeriodeMedAktivitetUføre(MorsAktivitetsTyper morsAktivitet) {
        return morsAktivitet != null && MorsAktivitet.UFØRE.getKode().equals(morsAktivitet.getKode());
    }

    private OppgittFordelingEntitet oversettFordeling(Behandling behandling,
                                                      Foreldrepenger omYtelse,
                                                      LocalDate mottattDato) {
        var oppgittePerioder = new ArrayList<>(omYtelse.getFordeling().getPerioder());
        var annenForelderErInformert = omYtelse.getFordeling().isAnnenForelderErInformert();
        var ønskerJustertVedFødsel = omYtelse.getFordeling().isOenskerJustertVedFoedsel();
        return lagOppgittFordeling(behandling, oppgittePerioder, annenForelderErInformert, mottattDato, ønskerJustertVedFødsel);
    }

    private OppgittFordelingEntitet lagOppgittFordeling(Behandling behandling,
                                                        List<LukketPeriodeMedVedlegg> perioder,
                                                        boolean annenForelderErInformert,
                                                        LocalDate mottattDatoFraSøknad,
                                                        Boolean ønskerJustertVedFødsel) {

        var oppgittPerioder = perioder.stream()
            .map(this::oversettPeriode)
            .filter(this::inneholderVirkedager)
            .toList();
        var filtrertPerioder = søknadDataFraTidligereVedtakTjeneste.filtrerVekkPerioderSomErLikeInnvilgetUttak(behandling, oppgittPerioder);
        var perioderMedTidligstMottatt = søknadDataFraTidligereVedtakTjeneste.oppdaterTidligstMottattDato(behandling, mottattDatoFraSøknad, filtrertPerioder);
        var perioderMedGodkjentVurdering = søknadDataFraTidligereVedtakTjeneste.oppdaterMedGodkjenteDokumentasjonsVurderinger(behandling, perioderMedTidligstMottatt);
        if (!inneholderVirkedager(perioderMedGodkjentVurdering)) {
            throw new IllegalArgumentException("Fordelingen må inneholde perioder med minst en virkedag");
        }
        return new OppgittFordelingEntitet(perioderMedGodkjentVurdering, annenForelderErInformert, Objects.equals(ønskerJustertVedFødsel, true));
    }

    private boolean inneholderVirkedager(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().anyMatch(this::inneholderVirkedager);
    }

    private boolean inneholderVirkedager(OppgittPeriodeEntitet periode) {
        return Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom()) > 0;
    }

    private Dekningsgrad oversettDekningsgrad(Foreldrepenger omYtelse) {
        var dekingsgrad = omYtelse.getDekningsgrad().getDekningsgrad();
        if (Integer.toString(Dekningsgrad._80.getVerdi()).equalsIgnoreCase(dekingsgrad.getKode())) {
            return Dekningsgrad._80;
        }
        if (Integer.toString(Dekningsgrad._100.getVerdi()).equalsIgnoreCase(dekingsgrad.getKode())) {
            return Dekningsgrad._100;
        }
        throw new IllegalArgumentException("Ukjent dekningsgrad " + dekingsgrad.getKode());
    }

    private OppgittPeriodeEntitet oversettPeriode(LukketPeriodeMedVedlegg lukketPeriode) {
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny().medPeriode(lukketPeriode.getFom(), lukketPeriode.getTom());
        if (lukketPeriode instanceof final Uttaksperiode periode) {
            oversettUttakperiode(oppgittPeriodeBuilder, periode);
        } else if (lukketPeriode instanceof Oppholdsperiode oppholdsperiode) {
            oppgittPeriodeBuilder.medÅrsak(OppholdÅrsak.fraKode(oppholdsperiode.getAarsak().getKode()));
            oppgittPeriodeBuilder.medPeriodeType(UttakPeriodeType.UDEFINERT);
        } else if (lukketPeriode instanceof Overfoeringsperiode overfoeringsperiode) {
            oppgittPeriodeBuilder.medÅrsak(OverføringÅrsak.fraKode(overfoeringsperiode.getAarsak().getKode()));
            oppgittPeriodeBuilder.medPeriodeType(UttakPeriodeType.fraKode(overfoeringsperiode.getOverfoeringAv().getKode()));
        } else if (lukketPeriode instanceof Utsettelsesperiode utsettelsesperiode) {
            oversettUtsettelsesperiode(oppgittPeriodeBuilder, utsettelsesperiode);
        } else {
            throw new IllegalStateException("Ukjent periodetype.");
        }
        return oppgittPeriodeBuilder.build();
    }

    private void oversettUtsettelsesperiode(OppgittPeriodeBuilder oppgittPeriodeBuilder,
                                            Utsettelsesperiode utsettelsesperiode) {
        if (utsettelsesperiode.getUtsettelseAv() != null) {
            oppgittPeriodeBuilder.medPeriodeType(
                UttakPeriodeType.fraKode(utsettelsesperiode.getUtsettelseAv().getKode()));
        }
        oppgittPeriodeBuilder.medÅrsak(UtsettelseÅrsak.fraKode(utsettelsesperiode.getAarsak().getKode()));
        if (utsettelsesperiode.getMorsAktivitetIPerioden() != null) {
            oppgittPeriodeBuilder.medMorsAktivitet(
                MorsAktivitet.fraKode(utsettelsesperiode.getMorsAktivitetIPerioden().getKode()));
        }
    }

    private void oversettUttakperiode(OppgittPeriodeBuilder oppgittPeriodeBuilder, Uttaksperiode periode) {
        oppgittPeriodeBuilder.medPeriodeType(UttakPeriodeType.fraKode(periode.getType().getKode()));
        if (periode.isOenskerFlerbarnsdager() != null) {
            oppgittPeriodeBuilder.medFlerbarnsdager(periode.isOenskerFlerbarnsdager());
        }
        //Støtter nå enten samtidig uttak eller gradering. Mulig dette endres senere
        if (erSamtidigUttak(periode)) {
            oppgittPeriodeBuilder.medSamtidigUttak(true);
            oppgittPeriodeBuilder.medSamtidigUttaksprosent(
                new SamtidigUttaksprosent(periode.getSamtidigUttakProsent()));
        } else if (periode instanceof Gradering gradering) {
            oversettGradering(oppgittPeriodeBuilder, gradering);
        }
        if (periode.getMorsAktivitetIPerioden() != null && !periode.getMorsAktivitetIPerioden().getKode().isEmpty()) {
            oppgittPeriodeBuilder.medMorsAktivitet(
                MorsAktivitet.fraKode(periode.getMorsAktivitetIPerioden().getKode()));
        }
    }

    private boolean erSamtidigUttak(Uttaksperiode periode) {
        return periode.isOenskerSamtidigUttak() != null && periode.isOenskerSamtidigUttak();
    }

    private void oversettGradering(OppgittPeriodeBuilder oppgittPeriodeBuilder, Gradering gradering) {
        var arbeidsgiverFraSøknad = gradering.getArbeidsgiver();
        if (arbeidsgiverFraSøknad != null) {
            var arbeidsgiver = oversettArbeidsgiver(arbeidsgiverFraSøknad);
            oppgittPeriodeBuilder.medArbeidsgiver(arbeidsgiver);
        }

        if (!gradering.isErArbeidstaker() && !gradering.isErFrilanser() && !gradering.isErSelvstNæringsdrivende()) {
            throw new IllegalArgumentException("Graderingsperioder må enten ha valgt at/fl/sn");
        }

        oppgittPeriodeBuilder.medGraderingAktivitetType(GraderingAktivitetType.from(gradering.isErArbeidstaker(), gradering.isErFrilanser(),
            gradering.isErSelvstNæringsdrivende()));
        oppgittPeriodeBuilder.medArbeidsprosent(BigDecimal.valueOf(gradering.getArbeidtidProsent()));
    }

    private Arbeidsgiver oversettArbeidsgiver(no.nav.vedtak.felles.xml.soeknad.uttak.v3.Arbeidsgiver arbeidsgiverFraSøknad) {
        if (arbeidsgiverFraSøknad instanceof Person) {
            var aktørId = personinfoAdapter.hentAktørForFnr(PersonIdent.fra(arbeidsgiverFraSøknad.getIdentifikator()));
            if (aktørId.isEmpty()) {
                throw new IllegalStateException("Finner ikke arbeidsgiver");
            }
            return Arbeidsgiver.person(aktørId.get());
        }
        if (arbeidsgiverFraSøknad instanceof Virksomhet) {
            var orgnr = arbeidsgiverFraSøknad.getIdentifikator();
            virksomhetTjeneste.hentOrganisasjon(orgnr);
            return Arbeidsgiver.virksomhet(orgnr);
        }
        throw new IllegalStateException("Ukjent arbeidsgiver type " + arbeidsgiverFraSøknad.getClass());
    }
}
