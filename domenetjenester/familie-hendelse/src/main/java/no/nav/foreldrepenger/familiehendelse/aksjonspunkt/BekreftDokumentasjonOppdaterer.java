package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftDokumentertDatoAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftDokumentertDatoAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftDokumentasjonOppdaterer implements AksjonspunktOppdaterer<BekreftDokumentertDatoAksjonspunktDto> {

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    BekreftDokumentasjonOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftDokumentasjonOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                          FamilieHendelseTjeneste familieHendelseTjeneste,
                                          SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.historikkAdapter = historikkAdapter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return !skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId).equals(forrigeSkjæringstidspunkt);
    }

    @Override
    public OppdateringResultat oppdater(BekreftDokumentertDatoAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();
        Behandling behandling = param.getBehandling();
        boolean totrinn = håndterEndringHistorikk(dto, param);

        // beregn denne før vi oppdaterer grunnlag
        final LocalDate forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);

        final FamilieHendelseBuilder oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .tilbakestillBarn()
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(dto.getOmsorgsovertakelseDato()));
        dto.getFodselsdatoer()
            .forEach((barnnummer, fødselsdato) -> oppdatertOverstyrtHendelse.leggTilBarn(new UidentifisertBarnEntitet(fødselsdato, barnnummer)));

        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);

        boolean skalReinnhente = skalReinnhenteRegisteropplysninger(behandlingId, forrigeSkjæringstidspunkt);

        if (skalReinnhente) {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).medOppdaterGrunnlag().build();
        } else {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
        }
    }

    private boolean håndterEndringHistorikk(BekreftDokumentertDatoAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        boolean erEndret;
        final FamilieHendelseGrunnlagEntitet hendelseGrunnlag = familieHendelseTjeneste.hentAggregat(param.getBehandlingId());

        LocalDate originalDato = getOmsorgsovertakelsesdatoForAdopsjon(
            hendelseGrunnlag.getGjeldendeAdopsjon().orElseThrow(IllegalStateException::new));
        erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.OMSORGSOVERTAKELSESDATO, originalDato, dto.getOmsorgsovertakelseDato());

        Map<Integer, LocalDate> orginaleFødselsdatoer = getAdopsjonFødselsdatoer(hendelseGrunnlag);
        Map<Integer, LocalDate> oppdaterteFødselsdatoer = dto.getFodselsdatoer();

        for (Map.Entry<Integer, LocalDate> entry : orginaleFødselsdatoer.entrySet()) {
            LocalDate oppdatertFødselsdato = oppdaterteFødselsdatoer.get(entry.getKey());
            erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.FODSELSDATO, entry.getValue(), oppdatertFødselsdato) || erEndret;
        }

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_ADOPSJON);
        return erEndret;
    }

    private LocalDate getOmsorgsovertakelsesdatoForAdopsjon(AdopsjonEntitet adopsjon) {
        return adopsjon.getOmsorgsovertakelseDato();
    }

    private Map<Integer, LocalDate> getAdopsjonFødselsdatoer(FamilieHendelseGrunnlagEntitet grunnlag) {
        return Optional.ofNullable(grunnlag.getGjeldendeBarna())
            .map(barna -> barna.stream()
                .collect(toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato)))
            .orElse(emptyMap());
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, LocalDate original, LocalDate bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }

}
