package no.nav.foreldrepenger.domene.opptjening.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.MapYrkesaktivitetTilOpptjeningsperiodeTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@ApplicationScoped
public class OpptjeningDtoTjeneste {
    private OpptjeningsperioderTjeneste forSaksbehandlingTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    OpptjeningDtoTjeneste() {
        // CDI
    }

    @Inject
    public OpptjeningDtoTjeneste(OpptjeningsperioderTjeneste forSaksbehandlingTjeneste,
                                     ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                     InntektArbeidYtelseTjeneste iayTjeneste) {
        this.forSaksbehandlingTjeneste = forSaksbehandlingTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.iayTjeneste = iayTjeneste;
    }

    public Optional<OpptjeningDto> mapFra(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        Optional<Opptjening> fastsattOpptjening = forSaksbehandlingTjeneste.hentOpptjeningHvisFinnes(behandlingId);

        OpptjeningDto resultat = new OpptjeningDto();
        if (fastsattOpptjening.isPresent() && fastsattOpptjening.get().getAktiv()) {
            List<OpptjeningAktivitet> opptjeningAktivitet = fastsattOpptjening.get().getOpptjeningAktivitet();
            resultat.setFastsattOpptjening(new FastsattOpptjeningDto(fastsattOpptjening.get().getFom(),
                fastsattOpptjening.get().getTom(), mapFastsattOpptjening(fastsattOpptjening.get()),
                MergeOverlappendePeriodeHjelp.mergeOverlappenePerioder(opptjeningAktivitet)));
        }
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlagOpt = iayTjeneste.finnGrunnlag(behandlingId);

        if (fastsattOpptjening.isPresent()) {
            resultat.setOpptjeningAktivitetList(forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, inntektArbeidYtelseGrunnlagOpt)
                .stream()
                .map(this::lagDtoFraOAPeriode)
                .collect(Collectors.toList()));
        } else {
            resultat.setOpptjeningAktivitetList(Collections.emptyList());
        }

        if (resultat.getFastsattOpptjening() == null && resultat.getOpptjeningAktivitetList().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(resultat);
    }

    private OpptjeningPeriodeDto mapFastsattOpptjening(Opptjening fastsattOpptjening) {
        return fastsattOpptjening.getOpptjentPeriode() != null ? new OpptjeningPeriodeDto(fastsattOpptjening.getOpptjentPeriode().getMonths(),
            fastsattOpptjening.getOpptjentPeriode().getDays()) : new OpptjeningPeriodeDto();
    }

    private OpptjeningAktivitetDto lagDtoFraOAPeriode(OpptjeningsperiodeForSaksbehandling oap) {
        var dto = new OpptjeningAktivitetDto(oap.getOpptjeningAktivitetType(),
            oap.getPeriode().getFomDato(), oap.getPeriode().getTomDato());

        var arbeidsgiver = oap.getArbeidsgiver();
        if (arbeidsgiver != null && arbeidsgiver.erAktørId()) {
            lagOpptjeningAktivitetDtoForPrivatArbeidsgiver(oap, dto);
        } else if (arbeidsgiver != null && OrganisasjonsNummerValidator.erGyldig(arbeidsgiver.getOrgnr())) {
            lagOpptjeningAktivitetDtoForArbeidsgiver(oap, dto, false);
        } else if (erKunstig(oap)) {
            lagOpptjeningAktivitetDtoForArbeidsgiver(oap, dto, true);
        } else {
            lagOpptjeningAktivitetDtoForUtlandskOrganisasjon(oap, dto);
        }
        settVurdering(oap, dto);
        leggPåFellesEgenskaper(oap, dto);
        return dto;
    }

    private void settVurdering(OpptjeningsperiodeForSaksbehandling oap, OpptjeningAktivitetDto dto) {
        if (oap.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)) {
            dto.setErGodkjent(true);
        } else if (oap.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)) {
            dto.setErGodkjent(false);
        }
    }

    private void leggPåFellesEgenskaper(OpptjeningsperiodeForSaksbehandling oap, OpptjeningAktivitetDto dto) {
        dto.setErManueltOpprettet(oap.getErManueltRegistrert());
        dto.setBegrunnelse(oap.getBegrunnelse());
        dto.setErEndret(oap.erManueltBehandlet());
        dto.setErPeriodeEndret(oap.getErPeriodeEndret());
        dto.setArbeidsforholdRef(Optional.ofNullable(oap.getOpptjeningsnøkkel())
            .flatMap(Opptjeningsnøkkel::getArbeidsforholdRef)
            .map(InternArbeidsforholdRef::getReferanse)
            .orElse(null));
    }

    private void lagOpptjeningAktivitetDtoForArbeidsgiver(OpptjeningsperiodeForSaksbehandling oap, OpptjeningAktivitetDto dto, boolean kunstig) {
        if (!kunstig && oap.getArbeidsgiver() != null && OpptjeningAktivitetType.NÆRING.equals(oap.getOpptjeningAktivitetType())) {
            var virksomhet = arbeidsgiverTjeneste.hentVirksomhet(oap.getArbeidsgiver().getOrgnr());
            dto.setNaringRegistreringsdato(virksomhet.getRegistrert());
        }
        dto.setArbeidsgiverReferanse(oap.getArbeidsgiver() != null ? oap.getArbeidsgiver().getIdentifikator() : null);
        dto.setStillingsandel(Optional.ofNullable(oap.getStillingsprosent()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO));
    }

    private void lagOpptjeningAktivitetDtoForUtlandskOrganisasjon(OpptjeningsperiodeForSaksbehandling oap, OpptjeningAktivitetDto dto) {
        if (oap.getArbeidsgiverUtlandNavn() != null) {
            dto.setArbeidsgiverReferanse(MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.lagReferanseForUtlandskOrganisasjon(oap.getArbeidsgiverUtlandNavn()));
        }
        dto.setStillingsandel(Optional.ofNullable(oap.getStillingsprosent()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO));
    }

    private void lagOpptjeningAktivitetDtoForPrivatArbeidsgiver(OpptjeningsperiodeForSaksbehandling oap, OpptjeningAktivitetDto dto) {
        dto.setArbeidsgiverReferanse(oap.getArbeidsgiver().getIdentifikator());
        dto.setStillingsandel(Optional.ofNullable(oap.getStillingsprosent()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO));
    }

    private boolean erKunstig(OpptjeningsperiodeForSaksbehandling oap) {
        return oap.getArbeidsgiver() != null && oap.getArbeidsgiver().getErVirksomhet() && Organisasjonstype.erKunstig(oap.getArbeidsgiver().getOrgnr());
    }

}
