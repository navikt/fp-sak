package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
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
                                                                               Behandling behandling) {
        if (AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
            List<OpptjeningsperiodeForSaksbehandling> aktivitetPerioder;
            var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
            var behandlingReferanse = BehandlingReferanse.fra(behandling);
            aktivitetPerioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingReferanse, skjæringstidspunkt);
            return aktivitetPerioder.stream()
                .filter(periode -> periode.erManueltBehandlet() || periode.getBegrunnelse() != null)
                .map(this::lagDtoAvPeriode)
                .toList();
        }
        return Collections.emptyList();
    }

    private TotrinnskontrollAktivitetDto lagDtoAvPeriode(OpptjeningsperiodeForSaksbehandling periode) {
        var dto = new TotrinnskontrollAktivitetDto();
        dto.setAktivitetType(periode.getOpptjeningAktivitetType().getNavn());
        dto.setErEndring(periode.getErPeriodeEndret());
        dto.setGodkjent(erPeriodeGodkjent(periode));

        Optional.ofNullable(periode.getArbeidsgiver()).ifPresent(a -> mapArbeidsgiverOpplysninger(dto, a));
        return dto;
    }

    private void mapArbeidsgiverOpplysninger(TotrinnskontrollAktivitetDto dto, Arbeidsgiver arbeidsgiver) {
        dto.setArbeidsgiverReferanse(arbeidsgiver.getIdentifikator());
        if (arbeidsgiver.erAktørId()) {
            var arbeidsgiverOpplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            if (arbeidsgiverOpplysninger != null) {
                dto.setPrivatpersonFødselsdato(arbeidsgiverOpplysninger.getFødselsdato());
                dto.setArbeidsgiverNavn(arbeidsgiverOpplysninger.getNavn());
            }
        } else if (arbeidsgiver.getOrgnr() != null) {
            var arbeidsgiverNavn = hentVirksomhetNavnPåOrgnr(arbeidsgiver.getOrgnr());
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
        return virksomhetTjeneste.finnOrganisasjon(orgnr).map(Virksomhet::getNavn)
            .orElseThrow(() -> new IllegalStateException("Finner ikke virksomhet med orgnr " + orgnr));
    }

}
