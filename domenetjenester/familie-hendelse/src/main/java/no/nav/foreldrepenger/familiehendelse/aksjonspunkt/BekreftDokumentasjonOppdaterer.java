package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftDokumentertDatoAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftDokumentertDatoAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftDokumentasjonOppdaterer implements AksjonspunktOppdaterer<BekreftDokumentertDatoAksjonspunktDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private Historikkinnslag2Repository historikkinnslag2Repository;

    BekreftDokumentasjonOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftDokumentasjonOppdaterer(FamilieHendelseTjeneste familieHendelseTjeneste,
                                          OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                          Historikkinnslag2Repository historikkinnslag2Repository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    @Override
    public OppdateringResultat oppdater(BekreftDokumentertDatoAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        var hendelseGrunnlag = familieHendelseTjeneste.hentAggregat(param.getBehandlingId());

        var originalOmsorgsovertakelse = getOmsorgsovertakelsesdatoForAdopsjon(hendelseGrunnlag.getGjeldendeAdopsjon().orElseThrow());
        var originalFødselsdatoer = getAdopsjonFødselsdatoer(hendelseGrunnlag);
        var erEndret = !Objects.equals(originalOmsorgsovertakelse, dto.getOmsorgsovertakelseDato()) || !Objects.equals(originalFødselsdatoer,
            dto.getFodselsdatoer()) || param.erBegrunnelseEndret();
        if (erEndret) {
            lagreHistorikk(param, dto, originalOmsorgsovertakelse, originalFødselsdatoer);
        }

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandlingId);
        oppdatertOverstyrtHendelse.tilbakestillBarn()
            .medAntallBarn(dto.getFodselsdatoer().keySet().size())
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder().medOmsorgsovertakelseDato(dto.getOmsorgsovertakelseDato()));
        dto.getFodselsdatoer()
            .forEach((barnnummer, fødselsdato) -> oppdatertOverstyrtHendelse.leggTilBarn(new UidentifisertBarnEntitet(fødselsdato, barnnummer)));

        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);

        var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        if (Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(erEndret).build();
        } else {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(erEndret).medOppdaterGrunnlag().build();
        }
    }

    private void lagreHistorikk(AksjonspunktOppdaterParameter param,
                                BekreftDokumentertDatoAksjonspunktDto dto,
                                LocalDate originalOmsorgsovertakelse,
                                Map<Integer, LocalDate> originaleFødselsdatoer) {


        var builder = new Historikkinnslag2.Builder().medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getBehandlingId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_ADOPSJON)
            .addTekstlinje(fraTilEquals("Omsorgsovertakelsesdato", originalOmsorgsovertakelse, dto.getOmsorgsovertakelseDato()));

        for (Map.Entry<Integer, LocalDate> eksisterendeFødselsdatoer : originaleFødselsdatoer.entrySet()) {
            var eksisterende = eksisterendeFødselsdatoer.getValue();
            var oppdatert = dto.getFodselsdatoer().get(eksisterendeFødselsdatoer.getKey());
            builder.addTekstlinje(fraTilEquals("Fødselsdato", eksisterende, oppdatert));
        }

        var historikkinnslag = builder.addTekstlinje(dto.getBegrunnelse()).build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    private LocalDate getOmsorgsovertakelsesdatoForAdopsjon(AdopsjonEntitet adopsjon) {
        return adopsjon.getOmsorgsovertakelseDato();
    }

    private Map<Integer, LocalDate> getAdopsjonFødselsdatoer(FamilieHendelseGrunnlagEntitet grunnlag) {
        return Optional.ofNullable(grunnlag.getGjeldendeBarna())
            .map(barna -> barna.stream().collect(toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato)))
            .orElse(emptyMap());
    }
}
