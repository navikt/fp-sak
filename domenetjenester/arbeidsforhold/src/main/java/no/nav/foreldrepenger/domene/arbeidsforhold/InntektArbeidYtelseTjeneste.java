package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public interface InntektArbeidYtelseTjeneste {

    /**
     * Hent grunnlag.
     *
     * @param behandlingId
     * @return henter aggregat, kaster feil hvis det ikke finnes.
     */
    InntektArbeidYtelseGrunnlag hentGrunnlag(Long behandlingId);

    /**
     * Hent grunnlag.
     *
     * @param behandlingId
     * @return henter aggregat uten å mappe om til internt objekt, kaster feil hvis det ikke finnes.
     */
    InntektArbeidYtelseGrunnlagDto hentGrunnlagKontrakt(Long behandlingId);

    /**
     * Hent grunnlag.
     *
     * @param behandlingUuid
     * @return henter aggregat, kaster feil hvis det ikke finnes.
     */
    InntektArbeidYtelseGrunnlag hentGrunnlag(UUID behandlingUuid);

    /**
     * Hent grunnlag gitt grunnlag id
     *
     * @param behandlingId
     * @param grunnlagUuid
     */
    InntektArbeidYtelseGrunnlag hentGrunnlagPåId(Long behandlingId, UUID grunnlagUuid);

    /**
     * Finn grunnlag hvis finnes
     *
     * @param behandlingId
     * @return henter optional aggregat
     */
    Optional<InntektArbeidYtelseGrunnlag> finnGrunnlag(Long behandlingId);

    /**
     *
     * @param behandlingId
     * @return Register inntekt og arbeid (Opprett for å endre eller legge til
     *         registeropplysning)
     */
    InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(Long behandlingId);

    /**
     *
     * @param behandlingUuid
     * @return Register inntekt og arbeid (Opprett for å endre eller legge til
     *         registeropplysning)
     */
    InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(UUID behandlingUuid, UUID angittReferanse, LocalDateTime angittOpprettetTidspunkt);

    /**
     * @param behandlingId
     * @return Saksbehanldet inntekt og arbeid (Opprett for å endre eller legge til
     *         saksbehanldet)
     */
    InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(Long behandlingId);

    /**
     * @param behandlingUuid
     * @return Saksbehanldet inntekt og arbeid (Opprett for å endre eller legge til
     *         saksbehanldet)
     */
    InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(UUID behandlingUuid, UUID angittReferanse,
            LocalDateTime angittOpprettetTidspunkt);

    /**
     * Lagre nytt grunnlag (gitt builder for å generere). Builder bør ikke
     * gjenbrukes etter å ha kalt her.
     *
     * @param behandlingId
     * @param inntektArbeidYtelseAggregatBuilder lagrer ned aggregat (builder
     *                                           bestemmer hvilke del av treet som
     *                                           blir lagret)
     */
    void lagreIayAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder);

    /**
     * @deprecated Denne blir lett misbrukt, siden man antagelig ønsker å gjøre mer
     *             enn kun fjerne saksbehandlet versjon. Bruk derfor heller
     *             {@link #lagreIayAggregat(Long, InntektArbeidYtelseAggregatBuilder)}
     *             etter du er ferdig med alle endringer du trenger å gjøre
     */
    @Deprecated
    void fjernSaksbehandletVersjon(Long behandlingId);

    /**
     * Lagre nytt grunnlag for Oppgitt Opptjening. Builder bør ikke gjenbrukes etter
     * kall her.
     */
    void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder);

    /**
     * Lagre nytt grunnlag for ArbeidsforholdInformasjon. Builder bør ikke
     * gjenbrukes etter kall her.
     *
     * @param behandlingId - Behandling Id
     * @param aktørId      - Aktør Id
     * @param builder      - {@link ArbeidsforholdInformasjonBuilder}
     */
    void lagreArbeidsforhold(Long behandlingId, AktørId aktørId, ArbeidsforholdInformasjonBuilder builder);

    /**
     * Kopier IAY grunnlag fra en behandling til en annen.
     *
     * @param fraBehandlingId - Kilde behandling
     * @param tilBehandlingId - Ny behandling
     */
    void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId);

    // Kun Oppgitt opptjening og inntektsmeldinger - ikke register eller overstyrt
    void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long fraBehandlingId, Long tilBehandlingId);

    List<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer);

    /**
     * Hent alle inntektsmeldinger registrert på saksnummer, indeksert på
     * behandling, grunnlag.
     *
     * @param saksnummer - Saksnummer
     */
    SakInntektsmeldinger hentInntektsmeldinger(Saksnummer saksnummer);


    /**
     * Lagrer arbeidsforholdene på IAY aggregatet
     *
     * @param behandlingId - Behandling Id
     * @param aktørId      - Aktør Id
     * @param builder      - {@link ArbeidsforholdInformasjonBuilder}
     */
    void lagreOverstyrtArbeidsforhold(Long behandlingId, AktørId aktørId, ArbeidsforholdInformasjonBuilder builder);

    /**
     * Lagre en eller flere inntektsmeldinger på en behandling for en sak.
     *
     * @param saksnummer   - Saksnummer
     * @param behandlingId - Behandling Id
     * @param builders     - Collection med {@link InntektsmeldingBuilder}
     */
    void lagreInntektsmeldinger(Saksnummer saksnummer, Long behandlingId, Collection<InntektsmeldingBuilder> builders);

    /**
     * Lagre en eller flere inntektsmeldinger på en behandling for en sak.
     *
     * @param referanse - behandlingreferanse
     */
    List<Inntektsmelding> finnInntektsmeldingDiff(BehandlingReferanse referanse);
}
