package no.nav.foreldrepenger.domene.entiteter;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

import java.util.Optional;


@Entity(name = "BeregningsgrunnlagGrunnlagEntitet")
@Table(name = "GR_BEREGNINGSGRUNNLAG")
public class BeregningsgrunnlagGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_BEREGNINGSGRUNNLAG")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @OneToOne
    @JoinColumn(name = "beregningsgrunnlag_id", updatable = false, unique = true)
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @ManyToOne
    @JoinColumn(name = "register_aktiviteter_id", updatable = false, unique = true)
    private BeregningAktivitetAggregatEntitet registerAktiviteter;

    @ManyToOne
    @JoinColumn(name = "saksbehandlet_aktiviteter_id", updatable = false, unique = true)
    private BeregningAktivitetAggregatEntitet saksbehandletAktiviteter;

    @ManyToOne
    @JoinColumn(name = "ba_overstyringer_id", updatable = false, unique = true)
    private BeregningAktivitetOverstyringerEntitet overstyringer;

    @ManyToOne
    @JoinColumn(name = "br_overstyringer_id", updatable = false, unique = true)
    private BeregningRefusjonOverstyringerEntitet refusjonOverstyringer;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Convert(converter= BeregningsgrunnlagTilstand.KodeverdiConverter.class)
    @Column(name="steg_opprettet", nullable = false)
    private BeregningsgrunnlagTilstand beregningsgrunnlagTilstand;

    public BeregningsgrunnlagGrunnlagEntitet() {
    }

    BeregningsgrunnlagGrunnlagEntitet(BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        this.beregningsgrunnlagTilstand = grunnlag.getBeregningsgrunnlagTilstand();
        this.behandlingId = grunnlag.getBehandlingId();
        grunnlag.getBeregningsgrunnlag().map(BeregningsgrunnlagEntitet::new).ifPresent(this::setBeregningsgrunnlag);
        this.setRegisterAktiviteter(grunnlag.getRegisterAktiviteter() == null ? null : new BeregningAktivitetAggregatEntitet(grunnlag.getRegisterAktiviteter()));
        grunnlag.getSaksbehandletAktiviteter().map(BeregningAktivitetAggregatEntitet::new).ifPresent(this::setSaksbehandletAktiviteter);
        grunnlag.getOverstyring().map(BeregningAktivitetOverstyringerEntitet::new).ifPresent(this::setOverstyringer);
        grunnlag.getRefusjonOverstyringer().map(BeregningRefusjonOverstyringerEntitet::new).ifPresent(this::setRefusjonOverstyringer);
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Optional<BeregningsgrunnlagEntitet> getBeregningsgrunnlag() {
        return Optional.ofNullable(beregningsgrunnlag);
    }

    public BeregningAktivitetAggregatEntitet getRegisterAktiviteter() {
        return registerAktiviteter;
    }

    public Optional<BeregningAktivitetAggregatEntitet> getSaksbehandletAktiviteter() {
        return Optional.ofNullable(saksbehandletAktiviteter);
    }

    public Optional<BeregningAktivitetOverstyringerEntitet> getOverstyring() {
        return Optional.ofNullable(overstyringer);
    }

    private Optional<BeregningAktivitetAggregatEntitet> getOverstyrteAktiviteter() {
        if (overstyringer != null) {
            var overstyrteAktiviteter = registerAktiviteter.getBeregningAktiviteter().stream()
                    .filter(beregningAktivitet -> beregningAktivitet.skalBrukes(overstyringer))
                    .toList();
            var overstyrtBuilder = BeregningAktivitetAggregatEntitet.builder()
                    .medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
            overstyrteAktiviteter.forEach(aktivitet -> {
                var overstyrtAktivitet = hentOverstyrtAktivitet(overstyringer, aktivitet);
                var kopiert = BeregningAktivitetEntitet.kopier(aktivitet)
                    .medPeriode(getIntervall(aktivitet, overstyrtAktivitet))
                    .build();
                overstyrtBuilder.leggTilAktivitet(kopiert);
            });
            return Optional.of(overstyrtBuilder.build());
        }
        return Optional.empty();
    }

    /**
     * Her hentes overstyrt aktivitet fra 'overstyringsAktiviteter' hvis 'aktivitet' finnes i listen av overstyringer
     * i 'overstyringsAktiviteter' (hvis nøklene deres er like). Hvis denne finnes så skal det altså bety at aktiveten
     * har blitt overstyrt, og hvis ikke, så har ikke aktiviteten overstyrt.
     *
     * @param overstyringsAktiviteter OverstyringAktiviteter
     * @param aktivitet Beregningaktivitet
     * @return En 'BeregningAktivitetOverstyringDto' hvis 'BeregningAktivitetDto' er overstyrt.
     */
    private Optional<BeregningAktivitetOverstyringEntitet> hentOverstyrtAktivitet(BeregningAktivitetOverstyringerEntitet overstyringsAktiviteter, BeregningAktivitetEntitet aktivitet) {
        return overstyringsAktiviteter
            .getOverstyringer()
            .stream()
            .filter(overstyrtAktivitet -> overstyrtAktivitet.getNøkkel().equals(aktivitet.getNøkkel()))
            .findFirst();
    }


        /**
         * Henter periode fra overstyrt aktivitet hvis aktivitet er overstyrt. Hvis ikke så hentes periode fra aktivitet.
         * @param aktivitet  beregningaktivitet
         * @param overstyring Gjeldende Overstyring
         * @return opprinnelig eller overstyrt intervall
         */
    private ÅpenDatoIntervallEntitet getIntervall(BeregningAktivitetEntitet aktivitet, Optional<BeregningAktivitetOverstyringEntitet> overstyring) {
        if (overstyring.isPresent()) {
            return overstyring.get().getPeriode();
        }
        return aktivitet.getPeriode();
    }

    public BeregningAktivitetAggregatEntitet getGjeldendeAktiviteter() {
        return getOverstyrteAktiviteter()
                .or(this::getSaksbehandletAktiviteter)
                .orElse(registerAktiviteter);
    }

    public BeregningAktivitetAggregatEntitet getOverstyrteEllerRegisterAktiviteter() {
        var overstyrteAktiviteter = getOverstyrteAktiviteter();
        if (overstyrteAktiviteter.isPresent()) {
            return overstyrteAktiviteter.get();
        }
        return registerAktiviteter;
    }

    public BeregningsgrunnlagTilstand getBeregningsgrunnlagTilstand() {
        return beregningsgrunnlagTilstand;
    }

    public boolean erAktivt() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    void setBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    void setRegisterAktiviteter(BeregningAktivitetAggregatEntitet registerAktiviteter) {
        this.registerAktiviteter = registerAktiviteter;
    }

    void setSaksbehandletAktiviteter(BeregningAktivitetAggregatEntitet saksbehandletAktiviteter) {
        this.saksbehandletAktiviteter = saksbehandletAktiviteter;
    }

    void setBeregningsgrunnlagTilstand(BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        this.beregningsgrunnlagTilstand = beregningsgrunnlagTilstand;
    }

    void setOverstyringer(BeregningAktivitetOverstyringerEntitet overstyringer) {
        this.overstyringer = overstyringer;
    }

    public Optional<BeregningRefusjonOverstyringerEntitet> getRefusjonOverstyringer() {
        return Optional.ofNullable(refusjonOverstyringer);
    }

    void setRefusjonOverstyringer(BeregningRefusjonOverstyringerEntitet refusjonOverstyringer) {
        this.refusjonOverstyringer = refusjonOverstyringer;
    }
}
