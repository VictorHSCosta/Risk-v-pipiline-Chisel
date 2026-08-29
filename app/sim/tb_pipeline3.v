`timescale 1ns/1ps

module tb_pipeline3;
  reg clock = 1'b0;
  reg reset = 1'b1;

  wire [31:0] io_pc;
  wire [31:0] io_instr;
  wire [31:0] io_aluResult;
  wire [31:0] io_writebackData;
  wire [4:0] io_writebackRd;
  wire io_writebackEnable;
  wire io_illegal;

  Pipeline3 dut (
    .clock(clock),
    .reset(reset),
    .io_pc(io_pc),
    .io_instr(io_instr),
    .io_aluResult(io_aluResult),
    .io_writebackData(io_writebackData),
    .io_writebackRd(io_writebackRd),
    .io_writebackEnable(io_writebackEnable),
    .io_illegal(io_illegal)
  );

  always #5 clock = ~clock;

  integer cycle;

  initial begin
    $dumpfile("output/pipeline.vcd");
    $dumpvars(0, tb_pipeline3);
    #20 reset = 1'b0;

    for (cycle = 0; cycle < 100; cycle = cycle + 1) begin
      @(posedge clock);
      $display("%0d pc=%08h instr=%08h wb=%b rd=%0d data=%08h illegal=%b",
        cycle, io_pc, io_instr, io_writebackEnable, io_writebackRd,
        io_writebackData, io_illegal);
      if (io_illegal) begin
        $display("ERRO: instrucao ilegal no ciclo %0d", cycle);
        $finish;
      end
    end

    $finish;
  end
endmodule
