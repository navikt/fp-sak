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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
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
    private BehandlingRepository behandlingRepository;

    BekreftDokumentasjonOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftDokumentasjonOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                          FamilieHendelseTjeneste familieHendelseTjeneste,
                                          SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste,
                                          BehandlingRepository behandlingRepository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.historikkAdapter = historikkAdapter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return !skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId).equals(forrigeSkjæringstidspunkt);
    }

    @Override
    public OppdateringResultat oppdater(BekreftDokumentertDatoAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var totrinn = håndterEndringHistorikk(dto, param);

        // beregn denne før vi oppdaterer grunnlag
        var forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .tilbakestillBarn()
            .medAntallBarn(dto.getFodselsdatoer().keySet().size())
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(dto.getOmsorgsovertakelseDato()));
        dto.getFodselsdatoer()
            .forEach((barnnummer, fødselsdato) -> oppdatertOverstyrtHendelse.leggTilBarn(new UidentifisertBarnEntitet(fødselsdato, barnnummer)));

        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);

        var skalReinnhente = skalReinnhenteRegisteropplysninger(behandlingId, forrigeSkjæringstidspunkt);

        if (skalReinnhente) {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).medOppdaterGrunnlag().build();
        }
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private boolean håndterEndringHistorikk(BekreftDokumentertDatoAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        boolean erEndret;
        var hendelseGrunnlag = familieHendelseTjeneste.hentAggregat(param.getBehandlingId());

        var originalDato = getOmsorgsovertakelsesdatoForAdopsjon(
            hendelseGrunnlag.getGjeldendeAdopsjon().orElseThrow(IllegalStateException::new));
        erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.OMSORGSOVERTAKELSESDATO, originalDato, dto.getOmsorgsovertakelseDato());

        var orginaleFødselsdatoer = getAdopsjonFødselsdatoer(hendelseGrunnlag);
        var oppdaterteFødselsdatoer = dto.getFodselsdatoer();

        for (var entry : orginaleFødselsdatoer.entrySet()) {
            var oppdatertFødselsdato = oppdaterteFødselsdatoer.get(entry.getKey());
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
