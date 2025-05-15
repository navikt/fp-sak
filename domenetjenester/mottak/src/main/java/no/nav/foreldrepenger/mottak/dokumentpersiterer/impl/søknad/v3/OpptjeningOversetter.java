package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import static java.util.Objects.nonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittUtenlandskVirksomhet;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Periode;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.AnnenOpptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.EgenNaering;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Frilans;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.NorskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Opptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskArbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Virksomhetstyper;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;

public class OpptjeningOversetter {

    private static final Logger LOG = LoggerFactory.getLogger(OpptjeningOversetter.class);

    private final VirksomhetTjeneste virksomhetTjeneste;
    private final InntektArbeidYtelseTjeneste iayTjeneste;

    public OpptjeningOversetter(VirksomhetTjeneste virksomhetTjeneste,
                                InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    void byggOpptjeningsspesifikkeFelter(SøknadWrapper skjemaWrapper, Behandling behandling) {
        var behandlingId = behandling.getId();
        Opptjening opptjeningFraSøknad = null;
        if (skjemaWrapper.getOmYtelse() instanceof final Foreldrepenger omYtelse) {
            opptjeningFraSøknad = omYtelse.getOpptjening();
        } else if (skjemaWrapper.getOmYtelse() instanceof final Svangerskapspenger omYtelse) {
            opptjeningFraSøknad = omYtelse.getOpptjening();
        }

        if (opptjeningFraSøknad != null && (!opptjeningFraSøknad.getUtenlandskArbeidsforhold().isEmpty()
            || !opptjeningFraSøknad.getAnnenOpptjening().isEmpty() || !opptjeningFraSøknad.getEgenNaering().isEmpty() || nonNull(
            opptjeningFraSøknad.getFrilans()))) {
            Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag = BehandlingType.REVURDERING.equals(behandling.getType()) ? iayTjeneste.finnGrunnlag(behandlingId) : Optional.empty();
            var eksisterendeOppgittOpptjening = iayGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getGjeldendeOppgittOpptjening);
            var erOverstyrt = iayGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getOverstyrtOppgittOpptjening).isPresent();
            if (eksisterendeOppgittOpptjening.isPresent()) {
                LOG.info("Fletter eksisterende oppgitt opptjening med ny data fra søknad for behandling med id {} ytelse {}", behandlingId, behandling.getFagsakYtelseType().getKode());
                var flettetOppgittOpptjening = flettOppgittOpptjening(opptjeningFraSøknad, eksisterendeOppgittOpptjening.get());
                var erEndringAvOppgittOpptjening = IAYGrunnlagDiff.erEndringPåOppgittOpptjening(eksisterendeOppgittOpptjening, Optional.of(flettetOppgittOpptjening.build()));
                if (!erEndringAvOppgittOpptjening) {
                    return;
                }
                if (erOverstyrt) {
                    iayTjeneste.lagreOppgittOpptjeningNullstillOverstyring(behandlingId, flettetOppgittOpptjening);
                } else {
                    iayTjeneste.lagreOppgittOpptjening(behandlingId, flettetOppgittOpptjening);
                }
            } else {
                var nyOppgittOpptjening = mapOppgittOpptjening(opptjeningFraSøknad);
                iayTjeneste.lagreOppgittOpptjening(behandlingId, nyOppgittOpptjening);
            }

        }
    }

    private OppgittOpptjeningBuilder flettOppgittOpptjening(Opptjening opptjening, OppgittOpptjening eksisterendeOppgittOpptjening) {
        // Bygger ny opptjening fra gammelt grunnlag for å ta vare på gamle opplysninger, så lenge equals metoder er rett vil ikke dette gi dobble innslag
        var flettetBuilder = OppgittOpptjeningBuilder.oppdater(Optional.of(eksisterendeOppgittOpptjening));
        var opptjeningFraNySøknad = mapOppgittOpptjening(opptjening).build();

        // Erstatter eksiterende frilans om finnes
        opptjeningFraNySøknad.getFrilans().ifPresent(flettetBuilder::leggTilFrilansOpplysninger);

        // Legger til nye perioder med annen aktivitet (type eller periode må være ulik)
        opptjeningFraNySøknad.getAnnenAktivitet().stream()
            .filter(aa -> !eksisterendeOppgittOpptjening.getAnnenAktivitet().contains(aa))
            .forEach(flettetBuilder::leggTilAnnenAktivitet);

        // Legger til nye næringer, eller erstatter næring med samme orgnr
        opptjeningFraNySøknad.getEgenNæring().forEach(flettetBuilder::leggTilEllerErstattEgenNæring);

        // Legger til nye perioder med oppgitt arbeidsforhold (type, utenlandskVirksomhet eller periode må være ulik)
        opptjeningFraNySøknad.getOppgittArbeidsforhold()
            .stream().filter(oa -> !eksisterendeOppgittOpptjening.getOppgittArbeidsforhold().contains(oa))
            .forEach(flettetBuilder::leggTilOppgittArbeidsforhold);

        return flettetBuilder;
    }

    private OppgittOpptjeningBuilder mapOppgittOpptjening(Opptjening opptjening) {
        var builder = OppgittOpptjeningBuilder.ny();
        opptjening.getAnnenOpptjening()
            .forEach(annenOpptjening -> builder.leggTilAnnenAktivitet(mapAnnenAktivitet(annenOpptjening)));
        opptjening.getEgenNaering().forEach(egenNaering -> builder.leggTilEgenNæring(mapEgenNæring(egenNaering)));
        opptjening.getUtenlandskArbeidsforhold()
            .forEach(arbeidsforhold -> builder.leggTilOppgittArbeidsforhold(
                mapOppgittUtenlandskArbeidsforhold(arbeidsforhold)));
        if (nonNull(opptjening.getFrilans())) {
            opptjening.getFrilans()
                .getPeriode()
                .forEach(periode -> builder.leggTilAnnenAktivitet(mapFrilansPeriode(periode)));
            builder.leggTilFrilansOpplysninger(mapFrilansOpplysninger(opptjening.getFrilans()));
        }
        return builder;
    }

    private OppgittFrilans mapFrilansOpplysninger(Frilans frilans) {
        var builder = OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
            .medErNyoppstartet(frilans.isErNyoppstartet())
            .medHarInntektFraFosterhjem(frilans.isHarInntektFraFosterhjem())
            .medHarNærRelasjon(frilans.isNaerRelasjon());
        frilans.getFrilansoppdrag().stream()
            .map(fo -> new OppgittFrilansoppdrag(fo.getOppdragsgiver(), mapPeriode(fo.getPeriode())))
            .forEach(builder::leggTilFrilansoppdrag);
        return builder.build();
    }

    private OppgittArbeidsforhold mapOppgittUtenlandskArbeidsforhold(
        UtenlandskArbeidsforhold utenlandskArbeidsforhold) {
        var builder = OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny();
        var landkode = finnLandkode(utenlandskArbeidsforhold.getArbeidsland().getKode());
        builder.medUtenlandskVirksomhet(
            new OppgittUtenlandskVirksomhet(landkode, utenlandskArbeidsforhold.getArbeidsgiversnavn()));
        builder.medErUtenlandskInntekt(true);
        builder.medArbeidType(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD);

        var periode = mapPeriode(utenlandskArbeidsforhold.getPeriode());
        builder.medPeriode(periode);
        return builder.build();
    }

    private OppgittAnnenAktivitet mapFrilansPeriode(Periode periode) {
        var datoIntervallEntitet = mapPeriode(periode);
        return new OppgittAnnenAktivitet(datoIntervallEntitet, ArbeidType.FRILANSER);
    }

    private OppgittAnnenAktivitet mapAnnenAktivitet(AnnenOpptjening annenOpptjening) {
        var datoIntervallEntitet = mapPeriode(annenOpptjening.getPeriode());
        var type = annenOpptjening.getType();

        var arbeidType = ArbeidType.fraKode(type.getKode());
        return new OppgittAnnenAktivitet(datoIntervallEntitet, arbeidType);
    }

    private List<OppgittOpptjeningBuilder.EgenNæringBuilder> mapEgenNæring(EgenNaering egenNæring) {
        List<OppgittOpptjeningBuilder.EgenNæringBuilder> builders = new ArrayList<>();
        egenNæring.getVirksomhetstype()
            .forEach(virksomhettype -> builders.add(mapEgenNæringForType(egenNæring, virksomhettype)));
        return builders;
    }

    private OppgittOpptjeningBuilder.EgenNæringBuilder mapEgenNæringForType(EgenNaering egenNæring,
                                                                            Virksomhetstyper virksomhettype) {
        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        if (egenNæring instanceof NorskOrganisasjon norskOrganisasjon) {
            var orgNr = norskOrganisasjon.getOrganisasjonsnummer();
            virksomhetTjeneste.hentOrganisasjon(orgNr);
            egenNæringBuilder.medVirksomhet(orgNr);
        } else {
            var utenlandskOrganisasjon = (UtenlandskOrganisasjon) egenNæring;
            var landkode = finnLandkode(utenlandskOrganisasjon.getRegistrertILand().getKode());
            egenNæringBuilder.medUtenlandskVirksomhet(
                new OppgittUtenlandskVirksomhet(landkode, utenlandskOrganisasjon.getNavn()));
        }

        // felles
        var virksomhetType = VirksomhetType.fraKode(virksomhettype.getKode());
        egenNæringBuilder.medPeriode(mapPeriode(egenNæring.getPeriode())).medVirksomhetType(virksomhetType);

        var regnskapsfoerer = Optional.ofNullable(egenNæring.getRegnskapsfoerer());
        regnskapsfoerer.ifPresent(
            r -> egenNæringBuilder.medRegnskapsførerNavn(r.getNavn()).medRegnskapsførerTlf(r.getTelefon()));

        egenNæringBuilder.medBegrunnelse(egenNæring.getBeskrivelseAvEndring())
            .medEndringDato(egenNæring.getEndringsDato())
            .medNyoppstartet(egenNæring.isErNyoppstartet())
            .medNyIArbeidslivet(egenNæring.isErNyIArbeidslivet())
            .medVarigEndring(egenNæring.isErVarigEndring())
            .medNærRelasjon(egenNæring.isNaerRelasjon() != null && egenNæring.isNaerRelasjon());
        if (egenNæring.getNaeringsinntektBrutto() != null) {
            egenNæringBuilder.medBruttoInntekt(new BigDecimal(egenNæring.getNaeringsinntektBrutto()));
        }
        return egenNæringBuilder;
    }

    private DatoIntervallEntitet mapPeriode(Periode periode) {
        var fom = periode.getFom();
        var tom = periode.getTom();
        if (tom == null) {
            return DatoIntervallEntitet.fraOgMed(fom);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    static Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }
}
