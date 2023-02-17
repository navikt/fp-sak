package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.FRILANS;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakYrkesaktiviteter;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetType;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppgittPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.SamtidigUttaksprosent;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Søknad;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Søknadstype;

@ApplicationScoped
public class SøknadGrunnlagBygger {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public SøknadGrunnlagBygger(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    SøknadGrunnlagBygger() {
        // CDI
    }

    public Søknad.Builder byggGrunnlag(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        return new Søknad.Builder()
            .type(type(input.getYtelsespesifiktGrunnlag()))
            .oppgittePerioder(oppgittePerioder(input, ytelseFordelingAggregat))
            .mottattTidspunkt(input.getSøknadOpprettetTidspunkt());
    }

    private List<OppgittPeriode> oppgittePerioder(UttakInput input, YtelseFordelingAggregat ytelseFordelingAggregat) {
        var oppgittePerioder =  ytelseFordelingAggregat.getGjeldendeFordeling().getPerioder();
        validerIkkeOverlappOppgittePerioder(oppgittePerioder);

        return oppgittePerioder.stream()
            .map(op -> byggOppgittperiode(op, new UttakYrkesaktiviteter(input).tilAktivitetIdentifikatorer()))
            .collect(Collectors.toList());
    }

    private OppgittPeriode byggOppgittperiode(OppgittPeriodeEntitet oppgittPeriode,
                                              Set<AktivitetIdentifikator> aktiviteter) {
        var oppgittPeriodeType = oppgittPeriode.getPeriodeType();
        var stønadskontotype = map(oppgittPeriodeType);

        final OppgittPeriode periode;
        if (UttakPeriodeType.STØNADSPERIODETYPER.contains(oppgittPeriodeType)) {
            if (oppgittPeriode.isUtsettelse()) {
                periode = byggUtsettelseperiode(oppgittPeriode);
            } else if (oppgittPeriode.isOverføring()) {
                periode = byggOverføringPeriode(oppgittPeriode, stønadskontotype);
            } else {
                periode = byggStønadsperiode(oppgittPeriode, stønadskontotype, aktiviteter);
            }
        } else if (UttakPeriodeType.ANNET.equals(oppgittPeriodeType)) {
            periode = byggTilOppholdPeriode(oppgittPeriode);
        } else {
            throw new IllegalArgumentException("Ikke-støttet UttakPeriodeType: " + oppgittPeriodeType);
        }
        return periode;
    }

    private static OppgittPeriode byggStønadsperiode(OppgittPeriodeEntitet oppgittPeriode,
                                                     Stønadskontotype stønadskontotype,
                                                     Set<AktivitetIdentifikator> aktiviter) {

        if (oppgittPeriode.isGradert()) {
            return byggGradertPeriode(oppgittPeriode, stønadskontotype, aktiviter);
        }
        return OppgittPeriode.forVanligPeriode(stønadskontotype, oppgittPeriode.getFom(), oppgittPeriode.getTom(),
            samtidigUttaksprosent(oppgittPeriode), oppgittPeriode.isFlerbarnsdager(),
            oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode), map(oppgittPeriode.getMorsAktivitet()),
            map(oppgittPeriode.getDokumentasjonVurdering()));
    }

    private static LocalDate tidligstMottattDato(OppgittPeriodeEntitet oppgittPeriode) {
        //Historiske behandlinger har ikke satt tidligstMottattDato - 22.4.2021
        return oppgittPeriode.getTidligstMottattDato().orElse(oppgittPeriode.getMottattDato());
    }

    private static SamtidigUttaksprosent samtidigUttaksprosent(OppgittPeriodeEntitet oppgittPeriode) {
        if (oppgittPeriode.getSamtidigUttaksprosent() == null) {
            return null;
        }
        //Ligger søknader fra tidligere i prod med samtidig uttak og 0%. Tolker som 100%
        if (oppgittPeriode.getSamtidigUttaksprosent().equals(no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent.ZERO)) {
            return SamtidigUttaksprosent.HUNDRED;
        }
        return new SamtidigUttaksprosent(oppgittPeriode.getSamtidigUttaksprosent().decimalValue());
    }

    private static OppgittPeriode byggGradertPeriode(OppgittPeriodeEntitet oppgittPeriode,
                                                     Stønadskontotype stønadskontotype,
                                                     Set<AktivitetIdentifikator> aktiviter) {
        var gradertAktivitet = finnGraderteAktiviteter(oppgittPeriode, aktiviter);
        if (gradertAktivitet.isEmpty()) {
            throw new IllegalStateException("Forventer minst en gradert aktivitet ved gradering i søknadsperioden");
        }

        return OppgittPeriode.forGradering(stønadskontotype, oppgittPeriode.getFom(), oppgittPeriode.getTom(),
            oppgittPeriode.getArbeidsprosent(), samtidigUttaksprosent(oppgittPeriode), oppgittPeriode.isFlerbarnsdager(),
            gradertAktivitet, oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode),
            map(oppgittPeriode.getMorsAktivitet()), UttakEnumMapper.map(oppgittPeriode.getDokumentasjonVurdering()));
    }

    private static Set<AktivitetIdentifikator> finnGraderteAktiviteter(OppgittPeriodeEntitet oppgittPeriode, Set<AktivitetIdentifikator> aktiviter) {
        if (oppgittPeriode.getGraderingAktivitetType() == FRILANS) {
            return aktivieterMedType(aktiviter, AktivitetType.FRILANS);
        }
        if (oppgittPeriode.getGraderingAktivitetType() == SELVSTENDIG_NÆRINGSDRIVENDE) {
            return aktivieterMedType(aktiviter, AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE);
        }
        return aktivieterMedType(aktiviter, AktivitetType.ARBEID).stream()
            .filter(aktivitetIdentifikator -> Objects.equals(oppgittPeriode.getArbeidsgiver().getIdentifikator(),
                Optional.ofNullable(aktivitetIdentifikator.getArbeidsgiverIdentifikator()).map(ai -> ai.value()).orElse(null)))
            .collect(Collectors.toSet());
    }

    private static Set<AktivitetIdentifikator> aktivieterMedType(Set<AktivitetIdentifikator> aktiviter, AktivitetType aktivitetType) {
        return aktiviter.stream().filter(aktivitetIdentifikator -> aktivitetIdentifikator.getAktivitetType().equals(aktivitetType)).collect(Collectors.toSet());
    }

    private static OppgittPeriode byggOverføringPeriode(OppgittPeriodeEntitet oppgittPeriode,
                                                        Stønadskontotype stønadskontotype) {
        var overføringÅrsak = map((OverføringÅrsak) oppgittPeriode.getÅrsak());

        return OppgittPeriode.forOverføring(stønadskontotype, oppgittPeriode.getFom(), oppgittPeriode.getTom(),
            overføringÅrsak, oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode), map(oppgittPeriode.getDokumentasjonVurdering()));
    }

    private static OppgittPeriode byggUtsettelseperiode(OppgittPeriodeEntitet oppgittPeriode) {
        var utsettelseÅrsak = map((UtsettelseÅrsak) oppgittPeriode.getÅrsak());

        return OppgittPeriode.forUtsettelse(oppgittPeriode.getFom(), oppgittPeriode.getTom(),
            utsettelseÅrsak, oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode),
            map(oppgittPeriode.getMorsAktivitet()), map(oppgittPeriode.getDokumentasjonVurdering()));
    }

    private static OppgittPeriode byggTilOppholdPeriode(OppgittPeriodeEntitet oppgittPeriode) {
        if (oppgittPeriode.isOpphold()) {
            var årsak = oppgittPeriode.getÅrsak();
            var oppholdÅrsak = (OppholdÅrsak) årsak;
            var mappedÅrsak = map(oppholdÅrsak);
            return OppgittPeriode.forOpphold(oppgittPeriode.getFom(), oppgittPeriode.getTom(),
                mappedÅrsak, oppgittPeriode.getMottattDato(), tidligstMottattDato(oppgittPeriode));
        }
        throw new IllegalArgumentException("Ikke-støttet årsakstype: " + oppgittPeriode.getÅrsak());
    }

    private static void validerIkkeOverlappOppgittePerioder(List<OppgittPeriodeEntitet> søknadPerioder) {
        var size = søknadPerioder.size();
        for (var i = 0; i < size; i++) {
            var periode1 = søknadPerioder.get(i);

            var p1 = new SimpleLocalDateInterval(periode1.getFom(), periode1.getTom());
            for (var j = i + 1; j < size; j++) {
                var periode2 = søknadPerioder.get(j);
                var p2 = new SimpleLocalDateInterval(periode2.getFom(), periode2.getTom());
                if (p1.overlapper(p2)) {
                    throw new IllegalStateException("Støtter ikke å ha overlappende søknadsperioder, men fikk overlapp mellom periodene " + p1 + " og " + p2);
                }
            }
        }
    }

    private static Søknadstype type(ForeldrepengerGrunnlag fpGrunnlag) {
        var hendelser = fpGrunnlag.getFamilieHendelser();
        if (hendelser.gjelderTerminFødsel() && hendelser.erSøktTermin()) {
            return Søknadstype.TERMIN;
        }
        if (hendelser.gjelderTerminFødsel()) {
            return Søknadstype.FØDSEL;
        }
        return Søknadstype.ADOPSJON;
    }

}
