package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollAktivitetDto;

@ApplicationScoped
public class TotrinnskontrollAktivitetDtoTjeneste {
    private OpptjeningsperioderTjeneste forSaksbehandlingTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected TotrinnskontrollAktivitetDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TotrinnskontrollAktivitetDtoTjeneste(OpptjeningsperioderTjeneste forSaksbehandlingTjeneste,
                                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                VirksomhetTjeneste virksomhetTjeneste,
                                                ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.forSaksbehandlingTjeneste = forSaksbehandlingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public List<TotrinnskontrollAktivitetDto> hentAktiviterEndretForOpptjening(Totrinnsvurdering aksjonspunkt,
                                                                               Behandling behandling,
                                                                               Optional<UUID> iayGrunnlagUuid) {
        if (AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
            List<OpptjeningsperiodeForSaksbehandling> aktivitetPerioder;
            LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();
            BehandlingReferanse behandlingReferanse = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
            if (iayGrunnlagUuid.isPresent()) {
                aktivitetPerioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingReferanse, iayGrunnlagUuid.get());
            } else {
                aktivitetPerioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingReferanse);
            }
            return aktivitetPerioder.stream()
                .filter(periode -> periode.erManueltBehandlet() || periode.getBegrunnelse() != null)
                .map(this::lagDtoAvPeriode)
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private TotrinnskontrollAktivitetDto lagDtoAvPeriode(OpptjeningsperiodeForSaksbehandling periode) {
        TotrinnskontrollAktivitetDto dto = new TotrinnskontrollAktivitetDto();
        dto.setAktivitetType(periode.getOpptjeningAktivitetType().getNavn());
        dto.setErEndring(periode.getErPeriodeEndret());
        dto.setGodkjent(erPeriodeGodkjent(periode));

        Arbeidsgiver arbeidsgiver = periode.getArbeidsgiver();
        if (arbeidsgiver != null) {
            mapArbeidsgiverOpplysninger(dto, arbeidsgiver);
        }
        return dto;
    }

    private void mapArbeidsgiverOpplysninger(TotrinnskontrollAktivitetDto dto, Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.erAktørId()) {
            ArbeidsgiverOpplysninger arbeidsgiverOpplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            if (arbeidsgiverOpplysninger != null) {
                dto.setPrivatpersonFødselsdato(arbeidsgiverOpplysninger.getFødselsdato());
                dto.setArbeidsgiverNavn(arbeidsgiverOpplysninger.getNavn());
            }
        } else if (arbeidsgiver.getOrgnr() != null) {
            String arbeidsgiverNavn = hentVirksomhetNavnPåOrgnr(arbeidsgiver.getOrgnr());
            dto.setArbeidsgiverNavn(arbeidsgiverNavn);
            dto.setOrgnr(arbeidsgiver.getOrgnr());
        }
    }

    private boolean erPeriodeGodkjent(OpptjeningsperiodeForSaksbehandling periode) {
        return VurderingsStatus.GODKJENT.equals(periode.getVurderingsStatus()) || VurderingsStatus.FERDIG_VURDERT_GODKJENT.equals(periode.getVurderingsStatus());
    }

    private String hentVirksomhetNavnPåOrgnr(String orgnr) {
        if (orgnr == null) {
            return null;
        }
        Optional<Virksomhet> virksomhet = virksomhetTjeneste.finnOrganisasjon(orgnr);
        if (!virksomhet.isPresent()) {
            virksomhet = virksomhetTjeneste.hentVirksomhet(orgnr);
        }
        return virksomhet.map(Virksomhet::getNavn)
            .orElseThrow(() -> new IllegalStateException("Finner ikke virksomhet med orgnr " + orgnr));
    }

}
